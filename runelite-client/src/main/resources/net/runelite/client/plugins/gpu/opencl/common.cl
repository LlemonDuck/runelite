/*
 * Copyright (c) 2021, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
#ifndef PI
#define PI 3.1415926535897932384626433832795f
#define UNIT PI / 1024.0f
#endif

float4 toScreen(int4 vertex, int cameraYaw, int cameraPitch, int centerX, int centerY, int zoom) {
	float yawSin = sin(cameraYaw * UNIT);
	float yawCos = cos(cameraYaw * UNIT);
	
	float pitchSin = sin(cameraPitch * UNIT);
	float pitchCos = cos(cameraPitch * UNIT);
	
	float rotatedX = (vertex.z * yawSin) + (vertex.x * yawCos);
	float rotatedZ = (vertex.z * yawCos) - (vertex.x * yawSin);
	
	float var13 = (vertex.y * pitchCos) - (rotatedZ * pitchSin);
	float var12 = (vertex.y * pitchSin) + (rotatedZ * pitchCos);
	
	float x = rotatedX * zoom / var12 + centerX;
	float y = var13 * zoom / var12 + centerY;
	float z = -var12;
	
	return (float4) (x, y, z, 0.0f);
}
 
/*
 * Rotate a vertex by a given orientation in JAU
 */
int4 rotateCustom(__global const struct uniform *uni, int4 vertex, int orientation) {
	int2 sinCos = uni->sinCosTable[2 * orientation]; // no fkn clue why 2* is needed hereig i
	int s = sinCos.x;
	int c = sinCos.y;
	int x = ((vertex.z * s) + (vertex.x * c)) >> 16;
	int z = ((vertex.z * c) - (vertex.x * s)) >> 16;
	return (int4)(x, vertex.y, z, vertex.w);
}

/*
 * Calculate the distance to a vertex given the camera angle
 */
int distanceCustom(int4 vertex, int cameraYaw, int cameraPitch) {
  int yawSin = (int)(65536.0f * sin(cameraYaw * UNIT));
  int yawCos = (int)(65536.0f * cos(cameraYaw * UNIT));

  int pitchSin = (int)(65536.0f * sin(cameraPitch * UNIT));
  int pitchCos = (int)(65536.0f * cos(cameraPitch * UNIT));

  int j = vertex.z * yawCos - vertex.x * yawSin >> 16;
  int l = vertex.y * pitchSin + j * pitchCos >> 16;

  return l;
}

/*
 * Calculate the distance to a face
 */
int face_distance(int4 vA, int4 vB, int4 vC, int cameraYaw, int cameraPitch) {
  int dvA = distanceCustom(vA, cameraYaw, cameraPitch);
  int dvB = distanceCustom(vB, cameraYaw, cameraPitch);
  int dvC = distanceCustom(vC, cameraYaw, cameraPitch);
  int faceDistance = (dvA + dvB + dvC) / 3;
  return faceDistance;
}

/*
 * Test if a face is visible (not backward facing)
 */
bool face_visible(__global const struct uniform *uni, int4 vA, int4 vB, int4 vC, int4 position) {
  // Move model to scene location, and account for camera offset
  int4 cameraPos = (int4)(uni->cameraX, uni->cameraY, uni->cameraZ, 0);
  vA += position - cameraPos;
  vB += position - cameraPos;
  vC += position - cameraPos;

  float4 sA = toScreen(vA, uni->cameraYaw, uni->cameraPitch, uni->centerX, uni->centerY, uni->zoom);
  float4 sB = toScreen(vB, uni->cameraYaw, uni->cameraPitch, uni->centerX, uni->centerY, uni->zoom);
  float4 sC = toScreen(vC, uni->cameraYaw, uni->cameraPitch, uni->centerX, uni->centerY, uni->zoom);

  return (sA.x - sB.x) * (sC.y - sB.y) - (sC.x - sB.x) * (sA.y - sB.y) > 0;
}

