package net.runelite.client.plugins.gpu.opencl;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Singleton;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.egl.EGLContext;
import jogamp.opengl.macosx.cgl.CGL;
import jogamp.opengl.windows.wgl.WindowsWGLContext;
import jogamp.opengl.x11.glx.X11GLXContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.gpu.GpuPlugin;
import net.runelite.client.plugins.gpu.template.Template;
import net.runelite.client.util.OSType;
import org.jocl.CL;
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

import static org.jocl.CL.*;

@Singleton
@Slf4j
public class OpenCLManager
{

	private static final int VAR_KERNEL_FACE_STRIDE = 8;
	private static final long FACES_PER_ITEM_UNORDERED = 6;
	private static final long FACES_PER_ITEM_SMALL = 512;
	private static final long FACES_PER_ITEM_LARGE = 4096;
	private static final String GL_SHARING_PLATFORM_EXT = "cl_khr_gl_sharing";
	private static final String MACOS_GL_SHARING_PLATFORM_EXT = "cl_APPLE_gl_sharing";

	private static final Template BASE_TEMPLATE =
		new Template().addInclude(OpenCLManager.class);

	static
	{
		BASE_TEMPLATE.add(key ->
		{
			if ("header".equals(key) || "prelude".equals(key))
			{
				return BASE_TEMPLATE.load("prelude.cl");
			}
			return null;
		});
	}

	private static final String SOURCE_COMPUTE_UNORDERED =
		BASE_TEMPLATE.load("comp_unordered.cl");
	
	private static final String SOURCE_COMPUTE_VARIABLE =
		BASE_TEMPLATE.load("comp_var.cl");

	private final int[] err = new int[1];

	private cl_platform_id platform;
	private cl_device_id device;
	private cl_context context;
	private cl_command_queue commandQueue;

	private cl_program programUnordered;
	private cl_program programVariable;

	private cl_kernel kernelUnordered;
	private cl_kernel kernelVariable;

	private cl_mem vertexBufferCL;
	private cl_mem uvBufferCL;

	private cl_mem tmpVertexBufferCL; // temporary scene vertex buffer
	private cl_mem tmpUvBufferCL; // temporary scene uv buffer
	private cl_mem tmpOutBufferCL; // target vertex buffer for compute shaders
	private cl_mem tmpOutUvBufferCL;

	private cl_mem tmpModelBufferCL; // scene model buffer, large
	private cl_mem tmpModelBufferSmallCL; // scene model buffer, small
	private cl_mem tmpModelBufferUnorderedCL;
	
	private cl_mem uniformBufferCL;
	
	private cl_mem[] lockedGLMemoryBuffers;
	private cl_event eventLockSharedBuffers;
	
	private int activeComputeLocks = 0;
	private cl_event[] computeLocks = new cl_event[4];

	private static final String KERNEL_NAME_UNORDERED = "computeUnordered";
	private static final String KERNEL_NAME_VARIABLE = "main";

	public void init(GL4 gl) throws OpenCLException
	{
		switch (OSType.getOSType()) {
			case Windows:
			case Linux:
				initPlatform();
				initDevice();
				initContext(gl);
				break;
			case MacOS:
				initMacOS(gl);
				break;
			default:
				throw new OpenCLException("Unsupported OS Type " + OSType.getOSType().name());
		}
		initQueue();
		compilePrograms();
	}

	public void cleanup()
	{
		Optional.ofNullable(commandQueue).ifPresent(CL::clFinish);
		
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
		
		Optional.ofNullable(uniformBufferCL).ifPresent(CL::clReleaseMemObject);
		uniformBufferCL = null;

		Optional.ofNullable(programUnordered).ifPresent(CL::clReleaseProgram);
		programUnordered = null;

		Optional.ofNullable(programVariable).ifPresent(CL::clReleaseProgram);
		programVariable = null;

		Optional.ofNullable(commandQueue).ifPresent(CL::clReleaseCommandQueue);
		commandQueue = null;

		Optional.ofNullable(kernelUnordered).ifPresent(CL::clReleaseKernel);
		kernelUnordered = null;

		Optional.ofNullable(kernelVariable).ifPresent(CL::clReleaseKernel);
		kernelVariable = null;

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
			
			long[] maxGroupSize = new long[1];
			clGetDeviceInfo(device, CL_DEVICE_MAX_WORK_GROUP_SIZE, Sizeof.size_t, Pointer.to(maxGroupSize), null);

			log.debug("Found cl_device_id {}", device);
			log.debug("DEVICE EXTENSIONS: {}", new String(devInfoBuf));
			log.debug("DEVICE MAX_WORK_GROUP_SIZE: {}", maxGroupSize[0]);
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

	private void initMacOS(GL4 gl) throws OpenCLException
	{
		// get sharegroup from gl context
		GLContext glContext = gl.getContext();
		if (!glContext.isCurrent())
			throw new OpenCLException("Can't create context from inactive GL");
		long cglContext = CGL.CGLGetCurrentContext();
		long cglShareGroup = CGL.CGLGetShareGroup(cglContext);
		
		// build context props
		cl_context_properties contextProps = new cl_context_properties();
		contextProps.addProperty(CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE, cglShareGroup);

		// ask macos to make the context for us
		log.debug("Creating context with props: {}", contextProps);
		context = clCreateContext(contextProps, 0, null, null, null, err);
		checkErr("Failed to create CLGL context");

		// pull the compute device out of the provided context
		device = new cl_device_id();
		clGetGLContextInfoAPPLE(context, cglContext, CL_CGL_DEVICE_FOR_CURRENT_VIRTUAL_SCREEN_APPLE, Sizeof.cl_device_id, Pointer.to(device), null);
		checkErr("Could not get device from CLGL context");

		log.debug("Got macOS CLGL compute device {}", device);
	}

	private void initQueue() throws OpenCLException
	{
		commandQueue = clCreateCommandQueue(context, device, 0, err);
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
//		programVariable = compileProgram(SOURCE_COMPUTE_VARIABLE);

		kernelUnordered = getKernel(programUnordered, KERNEL_NAME_UNORDERED);
//		kernelVariable = getKernel(programVariable, KERNEL_NAME_VARIABLE);
	}
	
	public void copyUniformBuffer(int uniformBuffer) throws OpenCLException
	{
		Optional.ofNullable(uniformBufferCL).ifPresent(CL::clReleaseMemObject);
		uniformBufferCL = null;
		
		uniformBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, uniformBuffer, err);
		checkErr("Couldn't copy uniform buffer");
	}

	public void copySceneBuffers(long vBufS, long uvBufS, int vertexBuffer, int uvBuffer) throws OpenCLException
	{
		Optional.ofNullable(vertexBufferCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(uvBufferCL).ifPresent(CL::clReleaseMemObject);
		vertexBufferCL = null;
		uvBufferCL = null;

		if (vBufS != 0 && uvBufS != 0)
		{
			vertexBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, vertexBuffer, err);
			checkErr("Couldn't copy vertexBuffer");

			uvBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, uvBuffer, err);
			checkErr("Couldn't copy uvBuffer");
		}
		else
		{
			log.warn("Could not copy 0-size buffer into opencl context");
		}
	}

	public void copyGLBuffers(int tmpVertexBuffer, int tmpUvBuffer, int tmpOutBuffer, int tmpOutUvBuffer) throws OpenCLException
	{
		Optional.ofNullable(tmpVertexBufferCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(tmpUvBufferCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(tmpOutBufferCL).ifPresent(CL::clReleaseMemObject);
		Optional.ofNullable(tmpOutUvBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpVertexBufferCL = null;
		tmpUvBufferCL = null;
		tmpOutBufferCL = null;
		tmpOutUvBufferCL = null;

		tmpVertexBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, tmpVertexBuffer, err);
		checkErr("Couldn't copy tmpVertexBuffer");

		tmpUvBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, tmpUvBuffer, err);
		checkErr("Couldn't copy tmpUvBuffer");

		tmpOutBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_WRITE, tmpOutBuffer, err);
		checkErr("Couldn't copy tmpOutBuffer");

		tmpOutUvBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_WRITE, tmpOutUvBuffer, err);
		checkErr("Couldn't copy tmpOutUvBuffer");
	}
	
	private void ensureSharedBuffersLocked() throws OpenCLException
	{
		if (eventLockSharedBuffers != null)
		{
			return;
		}
		
		lockedGLMemoryBuffers = new cl_mem[] {
			vertexBufferCL,
			tmpVertexBufferCL,
			uvBufferCL,
			tmpUvBufferCL,
			tmpOutBufferCL,
			tmpOutUvBufferCL,
//			uniformBufferCL,
		};
		if (Arrays.stream(lockedGLMemoryBuffers).anyMatch(Objects::isNull))
		{
			throw new OpenCLException("Called lock on null memory buffers");
		}

		eventLockSharedBuffers = new cl_event();
		err[0] = clEnqueueAcquireGLObjects(commandQueue, lockedGLMemoryBuffers.length, lockedGLMemoryBuffers, 0, null, eventLockSharedBuffers);
		checkErr("Couldn't lock shared buffers");
		computeLocks[activeComputeLocks++] = eventLockSharedBuffers;
	}
	
	private void setArgs(cl_kernel kernel, int base, cl_mem... argPointers) throws OpenCLException
	{
		for (int i = 0; i < argPointers.length; i++)
		{
			err[0] = clSetKernelArg(kernel, base + i, Sizeof.cl_mem, Pointer.to(argPointers[i]));
			checkErr("Couldn't set kernel argument " + (base + i) + " for kernel " + kernel);
		}
	}

	public void pushComputeUnordered(int unorderedCount, int modelBufferUnordered) throws OpenCLException
	{
		if (unorderedCount == 0)
		{
			return;
		}

		// lock unordered-specific buffer
		Optional.ofNullable(tmpModelBufferUnorderedCL).ifPresent(CL::clReleaseMemObject);
		tmpModelBufferUnorderedCL = null; // null-set in case of error locking
		tmpModelBufferUnorderedCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, modelBufferUnordered, err);
		checkErr("Could not copy modelBufferUnordered");
		
		// lock shared buffers
		ensureSharedBuffersLocked();
		
		cl_event lockU = new cl_event();
		err[0] = clEnqueueAcquireGLObjects(commandQueue, 1, new cl_mem[]{tmpModelBufferUnorderedCL}, 0, null, lockU);
		checkErr("Could not lock modelBufferUnordered");
		
		// queue compute call after lockU + shared lock
		cl_event compute = new cl_event();
		try
		{
			setArgs(kernelUnordered, 0, tmpModelBufferUnorderedCL, vertexBufferCL, tmpVertexBufferCL, tmpOutBufferCL, tmpOutUvBufferCL, uvBufferCL, tmpUvBufferCL);
			err[0] = clEnqueueNDRangeKernel(commandQueue, kernelUnordered, 1, null, new long[]{unorderedCount * FACES_PER_ITEM_UNORDERED}, new long[]{FACES_PER_ITEM_UNORDERED}, 2, new cl_event[]{eventLockSharedBuffers, lockU}, compute);
			checkErr("Could not enqueue compute order unordered");
			computeLocks[activeComputeLocks++] = compute;
		}
		finally
		{
			cl_event[] waitOn = compute.getNativePointer() != 0 ? new cl_event[]{compute} : new cl_event[] {lockU};
			err[0] = clEnqueueReleaseGLObjects(commandQueue, 1, new cl_mem[]{tmpModelBufferUnorderedCL}, 1, waitOn, null);
			checkErr("Could not release modelBufferUnordered");
		}
	}

	public void pushComputeSmall(int smallCount, int modelBufferSmall) throws OpenCLException
	{
		if (smallCount == 0)
		{
			return;
		}
		
		// lock shared buffers
		ensureSharedBuffersLocked();
		
		// lock small-specific buffer
		Optional.ofNullable(tmpModelBufferSmallCL).ifPresent(CL::clReleaseMemObject);
		tmpModelBufferSmallCL = null; // null-set in case of error locking
		tmpModelBufferSmallCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, modelBufferSmall, err);
		checkErr("Could not copy modelBufferSmall");
		
		cl_event lockS = new cl_event();
		err[0] = clEnqueueAcquireGLObjects(commandQueue, 1, new cl_mem[]{tmpModelBufferSmallCL}, 0, null, lockS);
		checkErr("Could not lock modelBufferSmall");
		
		// queue compute call after lockS + shared lock
		cl_event compute = new cl_event();
		try
		{
			clSetKernelArg(kernelVariable, 0, (12 + 12 + 18 + 1 + (FACES_PER_ITEM_SMALL)) * 4, null);
			setArgs(kernelVariable, 1, tmpModelBufferSmallCL, vertexBufferCL, tmpVertexBufferCL, tmpOutBufferCL, tmpOutUvBufferCL, uvBufferCL, tmpUvBufferCL, uniformBufferCL);
			long workGroupSize = FACES_PER_ITEM_SMALL / VAR_KERNEL_FACE_STRIDE;
			err[0] = clEnqueueNDRangeKernel(commandQueue, kernelVariable, 1, null, new long[]{smallCount * workGroupSize}, new long[]{workGroupSize}, 2, new cl_event[]{eventLockSharedBuffers, lockS}, compute);
			checkErr("Could not enqueue compute order small");
			computeLocks[activeComputeLocks++] = compute;
		}
		finally
		{
			cl_event[] waitOn = compute.getNativePointer() != 0 ? new cl_event[]{compute} : new cl_event[] {lockS};
			err[0] = clEnqueueReleaseGLObjects(commandQueue, 1, new cl_mem[]{tmpModelBufferSmallCL}, 1, waitOn, null);
			checkErr("Could not release modelBufferSmall");
		}
	}

	public void pushComputeLarge(int largeCount, int modelBufferLarge) throws OpenCLException
	{
		if (largeCount == 0)
		{
			return;
		}
		
		// lock shared buffers
		ensureSharedBuffersLocked();
		
		// lock small-specific buffer
		Optional.ofNullable(tmpModelBufferCL).ifPresent(CL::clReleaseMemObject);
		tmpModelBufferCL = null; // null-set in case of error locking
		tmpModelBufferCL = clCreateFromGLBuffer(context, CL_MEM_READ_ONLY, modelBufferLarge, err);
		checkErr("Could not copy modelBufferLarge");
		
		cl_event lockL = new cl_event();
		err[0] = clEnqueueAcquireGLObjects(commandQueue, 1, new cl_mem[]{tmpModelBufferCL}, 0, null, lockL);
		checkErr("Could not lock modelBufferLarge");
		
		// queue compute call after lockS + shared lock
		cl_event compute = new cl_event();
		try
		{
			clSetKernelArg(kernelVariable, 0, (12 + 12 + 18 + 1 + (FACES_PER_ITEM_LARGE)) * 4, null);
			setArgs(kernelVariable, 1, tmpModelBufferCL, vertexBufferCL, tmpVertexBufferCL, tmpOutBufferCL, tmpOutUvBufferCL, uvBufferCL, tmpUvBufferCL, uniformBufferCL);
			long workGroupSize = FACES_PER_ITEM_LARGE / VAR_KERNEL_FACE_STRIDE;
			err[0] = clEnqueueNDRangeKernel(commandQueue, kernelVariable, 1, null, new long[]{largeCount * workGroupSize}, new long[]{workGroupSize}, 2, new cl_event[]{eventLockSharedBuffers, lockL}, compute);
			checkErr("Could not enqueue compute order large");
			computeLocks[activeComputeLocks++] = compute;
		}
		finally
		{
			cl_event[] waitOn = compute.getNativePointer() != 0 ? new cl_event[]{compute} : new cl_event[] {lockL};
			err[0] = clEnqueueReleaseGLObjects(commandQueue, 1, new cl_mem[]{tmpModelBufferCL}, 1, waitOn, null);
			checkErr("Could not release modelBufferLarge");
		}
	}

	private void releaseSharedGLMemoryLocks() throws OpenCLException
	{
		err[0] = clEnqueueReleaseGLObjects(commandQueue, lockedGLMemoryBuffers.length, lockedGLMemoryBuffers, activeComputeLocks, computeLocks, null);
		checkErr("Couldn't release shared gl buffers");
	}
	
	public void finish() throws OpenCLException 
	{
		// each compute call should release its own model buffer
		releaseSharedGLMemoryLocks();
		activeComputeLocks = 0;
		eventLockSharedBuffers = null;

		err[0] = clFinish(commandQueue);
		checkErr("Could not synchronize with end of CL compute call");
	}

}
