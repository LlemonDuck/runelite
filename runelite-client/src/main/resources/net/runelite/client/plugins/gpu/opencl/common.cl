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
 
#include to_screen.cl

/*
 * Rotate a vertex by a given orientation in JAU
 */
int4 rotate(int4 vertex, int orientation) {
	int2 sinCos = sinCosTable[orientation];
	int s = sinCos.x;
	int c = sinCos.y;
	int x = vertex.z * s + vertex.x * c >> 16;
	int z = vertex.z * c - vertex.x * s >> 16;
	return (int4)(x, vertex.y, z, vertex.w);
}

/*
 * Calculate the distance to a vertex given the camera angle
 */
int distance(int4 vertex, int cameraYaw, int cameraPitch) {
  int yawSin = int(65536.0f * sin(cameraYaw * UNIT));
  int yawCos = int(65536.0f * cos(cameraYaw * UNIT));

  int pitchSin = int(65536.0f * sin(cameraPitch * UNIT));
  int pitchCos = int(65536.0f * cos(cameraPitch * UNIT));

  int j = vertex.z * yawCos - vertex.x * yawSin >> 16;
  int l = vertex.y * pitchSin + j * pitchCos >> 16;

  return l;
}

/*
 * Calculate the distance to a face
 */
int face_distance(int4 vA, int4 vB, int4 vC, int cameraYaw, int cameraPitch) {
  int dvA = distance(vA, cameraYaw, cameraPitch);
  int dvB = distance(vB, cameraYaw, cameraPitch);
  int dvC = distance(vC, cameraYaw, cameraPitch);
  int faceDistance = (dvA + dvB + dvC) / 3;
  return faceDistance;
}

/*
 * Test if a face is visible (not backward facing)
 */
bool face_visible(int4 vA, int4 vB, int4 vC, int4 position) {
  // Move model to scene location, and account for camera offset
  int4 cameraPos = (int4)(cameraX, cameraY, cameraZ, 0);
  vA += position - cameraPos;
  vB += position - cameraPos;
  vC += position - cameraPos;

  float4 sA = toScreen(vA, cameraYaw, cameraPitch, centerX, centerY, zoom);
  float4 sB = toScreen(vB, cameraYaw, cameraPitch, centerX, centerY, zoom);
  float4 sC = toScreen(vC, cameraYaw, cameraPitch, centerX, centerY, zoom);

  return (sA.x - sB.x) * (sC.y - sB.y) - (sC.x - sB.x) * (sA.y - sB.y) > 0;
}

