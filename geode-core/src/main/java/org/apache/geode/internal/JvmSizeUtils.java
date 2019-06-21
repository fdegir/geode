/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal;


import org.apache.commons.lang3.JavaVersion;

import org.apache.geode.annotations.Immutable;
import org.apache.geode.internal.lang.SystemUtils;
import org.apache.geode.pdx.internal.unsafe.UnsafeWrapper;

/**
 * The class provides info about some JVM characteristics
 */
public class JvmSizeUtils {

  private static final boolean is64Bit;
  private static final int referenceSize;
  private static final int objectHeaderSize;

  @Immutable
  private static final UnsafeWrapper unsafe;
  static {
    UnsafeWrapper tmp = null;
    try {
      tmp = new UnsafeWrapper();
    } catch (RuntimeException ignore) {
    } catch (Error ignore) {
    }
    unsafe = tmp;
  }

  static {
    int sunbits = Integer.getInteger("sun.arch.data.model", 0).intValue(); // also used by JRockit
    if (sunbits == 64) {
      is64Bit = true;
    } else if (sunbits == 32) {
      is64Bit = false;
    } else {
      int ibmbits = Integer.getInteger("com.ibm.vm.bitmode", 0).intValue();
      if (ibmbits == 64) {
        is64Bit = true;
      } else if (ibmbits == 32) {
        is64Bit = false;
      } else {
        if (unsafe != null) {
          is64Bit = unsafe.getAddressSize() == 8;
        } else {
          is64Bit = false;
        }
      }
    }
    if (!is64Bit) {
      referenceSize = 4;
      objectHeaderSize = 8;
    } else {
      int scaleIndex = 0;
      int tmpReferenceSize = 0;
      int tmpObjectHeaderSize = 0;
      if (SystemUtils.isAzulJVM()) {
        tmpObjectHeaderSize = 8;
        tmpReferenceSize = 8;
      } else {
        if (unsafe != null) {
          // Use unsafe to figure out the size of an object reference since we might
          // be using compressed oops.
          // Note: as of java 8 compressed oops do not imply a compressed object header.
          // The object header is determined by UseCompressedClassPointers.
          // UseCompressedClassPointers requires UseCompressedOops
          // but UseCompressedOops does not require UseCompressedClassPointers.
          // But it seems unlikely that someone would compress their oops
          // not their class pointers.
          scaleIndex = unsafe.arrayScaleIndex(Object[].class);
          if (scaleIndex == 4) {
            // compressed oops
            tmpReferenceSize = 4;
            tmpObjectHeaderSize = 12;
          } else if (scaleIndex == 8) {
            tmpReferenceSize = 8;
            tmpObjectHeaderSize = 16;
          } else {
            System.out.println("Unexpected arrayScaleIndex " + scaleIndex
                + ". Using max heap size to estimate reference size.");
            scaleIndex = 0;
          }
        }
        if (scaleIndex == 0) {
          // If our heap is > 32G (64G on java 8) then assume large oops. Otherwise assume
          // compressed oops.
          long SMALL_OOP_BOUNDARY = 32L;
          if (org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8)) {
            SMALL_OOP_BOUNDARY = 64L;
          }
          if (Runtime.getRuntime().maxMemory() > (SMALL_OOP_BOUNDARY * 1024 * 1024 * 1024)) {
            tmpReferenceSize = 8;
            tmpObjectHeaderSize = 16;
          } else {
            tmpReferenceSize = 4;
            tmpObjectHeaderSize = 12;
          }
        }
      }
      referenceSize = tmpReferenceSize;
      objectHeaderSize = tmpObjectHeaderSize;
    }
  }

  /**
   * @return true if this process is 64bit
   * @throws RuntimeException if sun.arch.data.model doesn't fit expectations
   */
  public static boolean is64Bit() {
    return is64Bit;
  }

  public static int getReferenceSize() {
    return referenceSize;
  }

  public static int getObjectHeaderSize() {
    return objectHeaderSize;
  }
}
