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
import bepal.eosio.crypto.util.HexUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * represents the result of a SHA256 hashing operation prefer to use the static
 * factory methods.
 */
public class Sha256 {

   public static final int HASH_LENGTH = 32;
   public static final Sha256 ZERO_HASH = new Sha256(new byte[HASH_LENGTH]);

   final private byte[] mHashBytes;

   /**
    * create Sha256 from raw hash bytes.
    * @param bytes
    */
   public Sha256(byte[] bytes) {
      Preconditions.checkArgument(bytes.length == HASH_LENGTH);
      this.mHashBytes = bytes;
   }

   private static MessageDigest getSha256Digest() {
      try {
         return MessageDigest.getInstance( "SHA-256" );
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); //cannot happen
      }
   }

   public static Sha256 from(byte[] data) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data, 0, data.length);
      return new Sha256(digest.digest());
   }

   public static Sha256 from(byte[] data, int offset, int length) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data, offset, length);
      return new Sha256(digest.digest());
   }

   public static Sha256 from(byte[] data1, byte[] data2) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data1, 0, data1.length);
      digest.update(data2, 0, data2.length);
      return new Sha256(digest.digest());

   }

   public static Sha256 doubleHash(byte[] data, int offset, int length) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data, offset, length);
      return new Sha256(digest.digest(digest.digest()));
   }



   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof Sha256))
         return false;
      return Arrays.equals(mHashBytes, ((Sha256) other).mHashBytes);
   }


   @Override
   public String toString() {
      return HexUtils.toHex(mHashBytes);
   }

   public byte[] getBytes() {
      return mHashBytes;
   }

   public boolean equalsFromOffset(byte[] toCompareData, int offsetInCompareData, int len ) {
      if ( ( null == toCompareData) || ( offsetInCompareData < 0)
              || ( len < 0) || ( mHashBytes.length <= len )
              || ( toCompareData.length <= offsetInCompareData) ) {
         return false;
      }

      for (int i = 0; i < len; i++) {

         if ( mHashBytes[i] != toCompareData[ offsetInCompareData + i] ) {
            return false;
         }
      }

      return true;
   }

   public int length() {
      return HASH_LENGTH;
   }
}
