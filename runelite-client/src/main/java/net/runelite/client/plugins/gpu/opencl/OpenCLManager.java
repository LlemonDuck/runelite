package net.runelite.client.plugins.gpu.opencl;

import com.google.common.io.CharStreams;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import javax.inject.Singleton;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.egl.EGLContext;
import jogamp.opengl.macosx.cgl.MacOSXCGLContext;
import jogamp.opengl.windows.wgl.WindowsWGLContext;
import jogamp.opengl.x11.glx.X11GLXContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.gpu.template.Template;
import org.jocl.CL;
import static org.jocl.CL.CL_CGL_SHAREGROUP_KHR;
import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_EXTENSIONS;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_EGL_DISPLAY_KHR;
import static org.jocl.CL.CL_GLX_DISPLAY_KHR;
import static org.jocl.CL.CL_GL_CONTEXT_KHR;
import static org.jocl.CL.CL_KERNEL_FUNCTION_NAME;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_PLATFORM_EXTENSIONS;
import static org.jocl.CL.CL_PLATFORM_NAME;
import static org.jocl.CL.CL_PLATFORM_PROFILE;
import static org.jocl.CL.CL_PLATFORM_VENDOR;
import static org.jocl.CL.CL_PLATFORM_VERSION;
import static org.jocl.CL.CL_PROGRAM_BINARY_TYPE;
import static org.jocl.CL.CL_PROGRAM_BUILD_LOG;
import static org.jocl.CL.CL_PROGRAM_BUILD_OPTIONS;
import static org.jocl.CL.CL_PROGRAM_BUILD_STATUS;
import static org.jocl.CL.CL_SUCCESS;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.CL_WGL_HDC_KHR;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateFromGLBuffer;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateKernelsInProgram;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueAcquireGLObjects;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clEnqueueReleaseGLObjects;
import static org.jocl.CL.clFinish;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetKernelInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clGetPlatformInfo;
import static org.jocl.CL.clGetProgramBuildInfo;
import static org.jocl.CL.clSetKernelArg;
import static org.jocl.CL.clWaitForEvents;
import static org.jocl.CL.stringFor_cl_build_status;
import static org.jocl.CL.stringFor_cl_platform_info;
import static org.jocl.CL.stringFor_cl_program_binary_type;
import static org.jocl.CL.stringFor_cl_program_build_info;
import static org.jocl.CL.stringFor_errorCode;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.jocl.cl_queue_properties;

@Singleton
@Slf4j
public class OpenCLManager
{

	private static final long WORK_ITEMS_PER_WORK_GROUP = 6;
	private static final int CONTEXT_PROPERTY_USE_CGL_APPLE = 268435456;
	private static final String GL_SHARING_PLATFORM_EXT = "cl_khr_gl_sharing";
	private static final String MACOS_GL_SHARING_PLATFORM_EXT = "cl_APPLE_gl_sharing";

	private static final Template BASE_TEMPLATE =
		new Template().addInclude(OpenCLManager.class);

	private static final String SOURCE_COMPUTE_UNORDERED =
		BASE_TEMPLATE.load("comp_unordered.cl");

	private final int[] err = new int[1];

	private cl_platform_id platform;
	private cl_device_id device;
	private cl_context context;
	private cl_command_queue commandQueue;

	private cl_program programUnordered;
	private cl_program programSmall;
	private cl_program programLarge;

	private cl_kernel kernelUnordered;
	private cl_kernel kernelSmall;
	private cl_kernel kernelLarge;

	private cl_mem vertexBufferCL;
	private cl_mem uvBufferCL;

	private cl_mem tmpVertexBufferCL; // temporary scene vertex buffer
	private cl_mem tmpUvBufferCL; // temporary scene uv buffer
	private cl_mem tmpOutBufferCL; // target vertex buffer for compute shaders
	private cl_mem tmpOutUvBufferCL;

	private cl_mem tmpModelBufferCL; // scene model buffer, large
	private cl_mem tmpModelBufferSmallCL; // scene model buffer, small
	private cl_mem tmpModelBufferUnorderedCL;

	@Getter
	private IntBuffer vertexBufferOut;

	@Getter
	private FloatBuffer uvBufferOut;

	private static final String KERNEL_NAME_UNORDERED = "computeUnordered";
	private static final String KERNEL_NAME_SMALL = "computeSmall";
	private static final String KERNEL_NAME_LARGE = "computeLarge";

	public void init(GL4 gl) throws OpenCLException
	{
		initPlatform();
		initDevice();
		initContext(gl);
		initQueue();
		compilePrograms();
	}

	public void cleanup()
	{
		Optional.ofNullable(vertexBufferCL).ifPresent(CL::clReleaseMemObject);
		vertexBufferCL = null;

		Optional.ofNullable(uvBufferCL).ifPresent(CL::clReleaseMemObject);
		uvBufferCL = null;

		Optional.ofNullable(tmpVertexBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpVertexBufferCL = null;

		Optional.ofNullable(tmpUvBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpUvBufferCL = null;

		Optional.ofNullable(tmpOutBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpOutBufferCL = null;

		Optional.ofNullable(tmpOutUvBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpOutUvBufferCL = null;

		Optional.ofNullable(tmpModelBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpModelBufferCL = null;

		Optional.ofNullable(tmpModelBufferSmallCL).ifPresent(CL::clReleaseMemObject);
		tmpModelBufferSmallCL = null;

		Optional.ofNullable(tmpModelBufferUnorderedCL).ifPresent(CL::clReleaseMemObject);
		tmpModelBufferUnorderedCL = null;

		Optional.ofNullable(programUnordered).ifPresent(CL::clReleaseProgram);
		programUnordered = null;

		Optional.ofNullable(programSmall).ifPresent(CL::clReleaseProgram);
		programSmall = null;

		Optional.ofNullable(programLarge).ifPresent(CL::clReleaseProgram);
		programLarge = null;

		Optional.ofNullable(commandQueue).ifPresent(CL::clReleaseCommandQueue);
		commandQueue = null;

		Optional.ofNullable(context).ifPresent(CL::clReleaseContext);
		context = null;

		Optional.ofNullable(device).ifPresent(CL::clReleaseDevice);
		device = null;
	}

	private void checkErr(String errorMsg) throws OpenCLException
	{
		if (err[0] != CL_SUCCESS)
		{
			log.error("{}: {}", errorMsg, stringFor_errorCode(err[0]));
			throw new OpenCLException(errorMsg);
		}
	}

	private String logPlatformInfo(cl_platform_id platform, int param)
	{
		long[] size = new long[1];
		clGetPlatformInfo(platform, param, 0, null, size);

		byte[] buffer = new byte[(int) size[0]];
		clGetPlatformInfo(platform, param, buffer.length, Pointer.to(buffer), null);
		String platformInfo = new String(buffer);
		log.debug("PLATFORM: {}, {}", stringFor_cl_platform_info(param), platformInfo);
		return platformInfo;
	}

	private void logBuildInfo(cl_program program, int param)
	{
		long[] size = new long[1];
		clGetProgramBuildInfo(program, device, param, 0, null, size);

		ByteBuffer buffer = ByteBuffer.allocateDirect((int) size[0]);
		clGetProgramBuildInfo(program, device, param, buffer.limit(), Pointer.toBuffer(buffer), null);

		switch (param)
		{
			case CL_PROGRAM_BUILD_STATUS:
				log.debug("COMPILE: {}, {}", stringFor_cl_program_build_info(param), stringFor_cl_build_status(buffer.getInt()));
				break;
			case CL_PROGRAM_BINARY_TYPE:
				log.debug("COMPILE: {}, {}", stringFor_cl_program_build_info(param), stringFor_cl_program_binary_type(buffer.getInt()));
				break;
			default:
				String message = StandardCharsets.US_ASCII.decode(buffer).toString();
				log.debug("COMPILE: {}, {}", stringFor_cl_program_build_info(param), message);
				break;
		}
	}

	private void initPlatform() throws OpenCLException
	{
		int[] platformCount = new int[1];
		err[0] = clGetPlatformIDs(0, null, platformCount);
		checkErr("Could not get compute platform");
		if (platformCount[0] == 0)
		{
			throw new OpenCLException("No compute platforms found");
		}

		cl_platform_id[] platforms = new cl_platform_id[platformCount[0]];
		err[0] = clGetPlatformIDs(platforms.length, platforms, null);
		checkErr("Could not get compute platform");

		for (cl_platform_id platform : platforms)
		{
			log.debug("Found cl_platform_id {}", platform);
			logPlatformInfo(platform, CL_PLATFORM_PROFILE);
			logPlatformInfo(platform, CL_PLATFORM_VERSION);
			logPlatformInfo(platform, CL_PLATFORM_NAME);
			logPlatformInfo(platform, CL_PLATFORM_VENDOR);
			String[] extensions = logPlatformInfo(platform, CL_PLATFORM_EXTENSIONS).split(" ");
			if (Arrays.stream(extensions).noneMatch(s -> s.equals(GL_SHARING_PLATFORM_EXT) || s.equals(MACOS_GL_SHARING_PLATFORM_EXT)))
				throw new OpenCLException("Platform does not support OpenGL buffer sharing");
		}

		platform = platforms[0];
		log.debug("Selected cl_platform_id {}", platform);
	}

	private void initDevice() throws OpenCLException
	{
		int[] deviceCount = new int[1];
		err[0] = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, deviceCount);
		checkErr("Could not get compute devices");
		if (deviceCount[0] == 0)
		{
			throw new OpenCLException("No compute devices found");
		}

		cl_device_id[] devices = new cl_device_id[(int) deviceCount[0]];
		err[0] = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);
		checkErr("Could not get compute devices");

		for (cl_device_id device : devices)
		{
			long[] size = new long[1];
			clGetDeviceInfo(device, CL_DEVICE_EXTENSIONS, 0, null, size);
			checkErr("Couldn't get device info");

			byte[] devInfoBuf = new byte[(int) size[0]];
			clGetDeviceInfo(device, CL_DEVICE_EXTENSIONS, devInfoBuf.length, Pointer.to(devInfoBuf), null);
			checkErr("Couldn't get device info");

			log.debug("Found cl_device_id {}", device);
			log.debug("DEVICE EXTENSIONS: {}", new String(devInfoBuf));
		}

		device = devices[0];
		log.debug("Selected cl_device_id {}", device);
	}

	private void initContext(GL4 gl) throws OpenCLException
	{
		// set computation platform
		cl_context_properties contextProps = new cl_context_properties();
		contextProps.addProperty(CL_CONTEXT_PLATFORM, platform);

		// pull gl context
		GLContext glContext = gl.getContext();
		log.debug("Got GLContext of type {}", glContext.getClass().getSimpleName());
		if (!glContext.isCurrent())
			throw new OpenCLException("Can't create OpenCL context from inactive GL Context");

		// get correct props based on os
		long glContextHandle = glContext.getHandle();
		GLContextImpl glContextImpl = (GLContextImpl)glContext;
		GLDrawableImpl glDrawableImpl = glContextImpl.getDrawableImpl();
		NativeSurface nativeSurface = glDrawableImpl.getNativeSurface();

		if (glContext instanceof X11GLXContext)
		{
			long displayHandle = nativeSurface.getDisplayHandle();
			contextProps.addProperty(CL_GL_CONTEXT_KHR, glContextHandle);
			contextProps.addProperty(CL_GLX_DISPLAY_KHR, displayHandle);
		}
		else if (glContext instanceof WindowsWGLContext)
		{
			long surfaceHandle = nativeSurface.getSurfaceHandle();
			contextProps.addProperty(CL_GL_CONTEXT_KHR, glContextHandle);
			contextProps.addProperty(CL_WGL_HDC_KHR, surfaceHandle);
		}
		else if (glContext instanceof MacOSXCGLContext)
		{
			contextProps.addProperty(CONTEXT_PROPERTY_USE_CGL_APPLE, glContextHandle);
		}
		else if (glContext instanceof EGLContext)
		{
			long displayHandle = nativeSurface.getDisplayHandle();
			contextProps.addProperty(CL_GL_CONTEXT_KHR, glContextHandle);
			contextProps.addProperty(CL_EGL_DISPLAY_KHR, displayHandle);
		}

		log.debug("Creating context with props: {}", contextProps);
		context = clCreateContext(contextProps, 1, new cl_device_id[]{device}, null, null, err);
		checkErr("Could not create compute context");
		log.debug("Created compute context {}", context);
	}

	private void initQueue() throws OpenCLException
	{
		commandQueue = clCreateCommandQueueWithProperties(context, device, new cl_queue_properties(), err);
		checkErr("Could not create command queue");
		log.debug("Created command_queue {}", commandQueue);
	}

	private cl_program compileProgram(String programSource) throws OpenCLException
	{
		log.trace("Compiling program:\n" + programSource);
		cl_program program = clCreateProgramWithSource(context, 1, new String[]{programSource}, null, err);
		checkErr("Failed to init program in context " + context);

		try
		{
			clBuildProgram(program, 0, null, null, null, null);
			checkErr("Could not compile program from source:\n" + programSource);
		} catch (OpenCLException e)
		{
			logBuildInfo(program, CL_PROGRAM_BUILD_LOG);
			throw e;
		}

		logBuildInfo(program, CL_PROGRAM_BUILD_STATUS);
		logBuildInfo(program, CL_PROGRAM_BINARY_TYPE);
		logBuildInfo(program, CL_PROGRAM_BUILD_OPTIONS);
		logBuildInfo(program, CL_PROGRAM_BUILD_LOG);
		return program;
	}

	private cl_kernel getKernel(cl_program program, String kernelName) throws OpenCLException
	{
		// print all kernels in program
		if (log.isDebugEnabled()) {
			int[] kernelCount = new int[1];
			err[0] = clCreateKernelsInProgram(program, 0, null, kernelCount);
			checkErr("Could not load kernels in program " + program);

			cl_kernel[] kernels = new cl_kernel[kernelCount[0]];
			err[0] = clCreateKernelsInProgram(program, kernels.length, kernels, null);
			checkErr("Could not load kernels in program " + program);

			for (cl_kernel k : kernels)
			{
				long[] size = new long[1];
				err[0] = clGetKernelInfo(k, CL_KERNEL_FUNCTION_NAME, 0, null, size);
				checkErr("Could not get kernel info for kernel " + k);

				byte[] kernelNameBuf = new byte[(int) size[0]];
				err[0] = clGetKernelInfo(k, CL_KERNEL_FUNCTION_NAME, kernelNameBuf.length, Pointer.to(kernelNameBuf), null);
				checkErr("Could not get kernel info for kernel " + k);
				String kName = new String(kernelNameBuf);
				log.debug("Found kernel " + kName + " in program " + program);
			}
		}

		cl_kernel kernel = clCreateKernel(program, kernelName, err);
		checkErr("Could not create kernel " + kernelName + " for program " + program);
		log.debug("Loaded kernel " + kernelName + " for program " + program);
		return kernel;
	}

	private void compilePrograms() throws OpenCLException
	{
		programUnordered = compileProgram(SOURCE_COMPUTE_UNORDERED);
//		compileProgram("comp_small.cl");
//		compileProgram("comp_large.cl");

		kernelUnordered = getKernel(programUnordered, "computeUnordered");
//		kernelSmall = getKernel(programSmall, "computeSmall");
//		kernelLarge = getKernel(programLarge, "computeLarge");
	}

	public void copySceneBuffers(int vertexBuffer, int uvBuffer) throws OpenCLException
	{
		Optional.ofNullable(vertexBufferCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(uvBufferCL).ifPresent(CL::clReleaseMemObject);
		vertexBufferCL = null;
		uvBufferCL = null;

		vertexBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, vertexBuffer, err);
		checkErr("Couldn't copy vertexBuffer");

		uvBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, uvBuffer, err);
		checkErr("Couldn't copy uvBuffer");
	}

	public void copyGLBuffers(int tmpVertexBuffer, int tmpUvBuffer, int tmpModelBuffer, int tmpModelBufferSmall, int tmpModelBufferUnordered, int tmpOutBuffer, int tmpOutUvBuffer) throws OpenCLException
	{
		Optional.ofNullable(tmpVertexBufferCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(tmpUvBufferCL).ifPresent(CL::clReleaseMemObject);
//		Optional.ofNullable(tmpModelBufferCL).ifPresent(CL::clReleaseMemObject);
//		Optional.ofNullable(tmpModelBufferSmallCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(tmpModelBufferUnorderedCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(tmpOutBufferCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(tmpOutUvBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpVertexBufferCL = null;
		tmpUvBufferCL = null;
//		tmpModelBufferCL = null;
//		tmpModelBufferSmallCL = null;
		tmpModelBufferUnorderedCL = null;
		tmpOutBufferCL = null;
		tmpOutUvBufferCL = null;

		tmpVertexBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, tmpVertexBuffer, err);
		checkErr("Couldn't copy tmpVertexBuffer");

		tmpUvBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, tmpUvBuffer, err);
		checkErr("Couldn't copy tmpUvBuffer");

//		tmpModelBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, tmpModelBuffer, err);
//		checkErr("Couldn't copy tmpModelBuffer");

//		tmpModelBufferSmallCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, tmpModelBufferSmall, err);
//		checkErr("Couldn't copy tmpModelBufferSmall");

		tmpModelBufferUnorderedCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, tmpModelBufferUnordered, err);
		checkErr("Couldn't copy tmpModelBufferUnordered");

		tmpOutBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_WRITE, tmpOutBuffer, err);
		checkErr("Couldn't copy tmpModelBufferUnordered");

		tmpOutUvBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_WRITE, tmpOutUvBuffer, err);
		checkErr("Couldn't copy tmpOutUvBuffer");
	}

	public void computeUnordered(int unorderedModels) throws OpenCLException
	{
		cl_mem[] glBuffers = {
			tmpModelBufferUnorderedCL,
			vertexBufferCL,
			tmpVertexBufferCL,
			uvBufferCL,
			tmpUvBufferCL,
			tmpOutBufferCL,
			tmpOutUvBufferCL,
		};

		cl_event acquireGLBuffers = new cl_event();
		clEnqueueAcquireGLObjects(commandQueue, glBuffers.length, glBuffers, 0, null, acquireGLBuffers);

		clSetKernelArg(kernelUnordered, 0, Sizeof.cl_mem, Pointer.to(tmpModelBufferUnorderedCL));
		clSetKernelArg(kernelUnordered, 1, Sizeof.cl_mem, Pointer.to(vertexBufferCL));
		clSetKernelArg(kernelUnordered, 2, Sizeof.cl_mem, Pointer.to(tmpVertexBufferCL));
		clSetKernelArg(kernelUnordered, 3, Sizeof.cl_mem, Pointer.to(uvBufferCL));
		clSetKernelArg(kernelUnordered, 4, Sizeof.cl_mem, Pointer.to(tmpUvBufferCL));
		clSetKernelArg(kernelUnordered, 5, Sizeof.cl_mem, Pointer.to(tmpOutBufferCL));
		clSetKernelArg(kernelUnordered, 6, Sizeof.cl_mem, Pointer.to(tmpOutUvBufferCL));

		// queue compute call after acquireGLBuffers
		cl_event compute = new cl_event();
		err[0] = clEnqueueNDRangeKernel(commandQueue, kernelUnordered, 1, null, new long[] { unorderedModels * WORK_ITEMS_PER_WORK_GROUP }, new long[] { WORK_ITEMS_PER_WORK_GROUP }, 1, new cl_event[]{acquireGLBuffers}, compute);
		checkErr("Could not enqueue compute order");

		// queue release call after compute
		clEnqueueReleaseGLObjects(commandQueue, glBuffers.length, glBuffers, 1, new cl_event[]{compute}, null);

		err[0] = clFinish(commandQueue);
		checkErr("Could not synchronize with end of CL compute call");
	}

}
