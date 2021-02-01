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

#include cl_types.cl
#include to_screen.cl
#include common.cl
#include priority_render.cl

__kernel
__attribute__((reqd_work_group_size(256, 1, 1)))
void computeSmall(__local struct shared_data *shared,
				  __global const struct modelinfo *ol,
                  __global const int4 *vb,
                  __global const int4 *tempvb,
                  __global const float4 *uv,
                  __global const float4 *tempuv,
                  __global int4 *vout,
                  __global float4 *uvout,
                  __global const struct uniform *uni) {
  size_t groupId = get_group_id(0);
  size_t localId = get_local_id(0);
  struct modelinfo minfo = ol[groupId];
  int4 pos = (int4)(minfo.x, minfo.y, minfo.z, 0);

  if (localId == 0) {
    shared->min10 = 1600;
    for (int i = 0; i < 12; ++i) {
      shared->totalNum[i] = 0;
      shared->totalDistance[i] = 0;
    }
    for (int i = 0; i < 18; ++i) {
      shared->totalMappedNum[i] = 0;
    }
  }

  int prio1, dis1;
  int4 vA1, vA2, vA3;

  get_face(shared, uni, vb, tempvb, localId, minfo, uni->cameraYaw, uni->cameraPitch, &prio1, &dis1, &vA1, &vA2, &vA3);

  barrier(CLK_LOCAL_MEM_FENCE | CLK_GLOBAL_MEM_FENCE);

  add_face_prio_distance(shared, uni, localId, minfo, vA1, vA2, vA3, prio1, dis1, pos);

  barrier(CLK_LOCAL_MEM_FENCE | CLK_GLOBAL_MEM_FENCE);

  int prio1Adj;
  int idx1 = map_face_priority(shared, localId, minfo, prio1, dis1, &prio1Adj);

  barrier(CLK_LOCAL_MEM_FENCE | CLK_GLOBAL_MEM_FENCE);

  insert_dfs(shared, localId, minfo, prio1Adj, dis1, idx1);

  barrier(CLK_LOCAL_MEM_FENCE | CLK_GLOBAL_MEM_FENCE);

  sort_and_insert(shared, uv, tempuv, vout, uvout, localId, minfo, prio1Adj, dis1, vA1, vA2, vA3);
}
