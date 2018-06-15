/*
* Copyright (c) 2018, Bepal
* All rights reserved.
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, Berkeley nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package bepal.eosio.crypto.digest;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import bepal.eosio.crypto.util.HexUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Sha512 implements Comparable<Sha512> {

   public static final int HASH_LENGTH = 64;
   public static final Sha512 ZERO_HASH = new Sha512(new byte[HASH_LENGTH]);

   final private byte[] mHashBytes;

   public Sha512(byte[] bytes) {
      Preconditions.checkArgument(bytes.length == HASH_LENGTH);
      this.mHashBytes = bytes;
   }

   public static Sha512 from(byte[] data) {
      MessageDigest digest;
      try {
         digest = MessageDigest.getInstance("SHA-512");
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); //cannot happen
      }

      digest.update(data, 0, data.length);

      return new Sha512(digest.digest());
   }


   private Sha512(byte[] bytes, int offset) {
      //defensive copy, since incoming bytes is of arbitrary length
      mHashBytes = new byte[HASH_LENGTH];
      System.arraycopy(bytes, offset, mHashBytes, 0, HASH_LENGTH);
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof Sha512))
         return false;
      return Arrays.equals(mHashBytes, ((Sha512) other).mHashBytes);
   }


   @Override
   public String toString() {
      return HexUtils.toHex(mHashBytes);
   }

   public byte[] getBytes() {
      return mHashBytes;
   }

   @Override
   public int compareTo(Sha512 o) {
      for (int i = 0; i < HASH_LENGTH; i++) {
         byte myByte = mHashBytes[i];
         byte otherByte = o.mHashBytes[i];

         final int compare = Ints.compare(myByte, otherByte);
         if (compare != 0)
            return compare;
      }
      return 0;
   }
}
