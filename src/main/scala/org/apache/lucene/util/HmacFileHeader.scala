/*
 * Copyright 2017 Workday, Inc.
 *
 * This software is available under the MIT license.
 * Please see the LICENSE.txt file in this project.
 */

package org.apache.lucene.util

import java.io.RandomAccessFile

import com.workday.elasticrypt.KeyProvider

/**
  * Implementation of the FileHeader interface that adds a MAC hash that is
  * used to verify that the correct key is being used to decrypt a file.
  */
class HmacFileHeader(raf: RandomAccessFile, keyProvider: KeyProvider, indexName: String) extends FileHeader(raf) {

  // scalastyle:off null
  private var hmacBytes: Array[Byte] = null
  private var plainTextBytes: Array[Byte] = null
  // scalastyle:on null

  /**
    * Writes the file header.
    * @return the resulting file pointer
    */
  def writeHeader(): Long = {
    // Write index name
    indexNameBytes = indexName.getBytes
    writeByteArray(indexNameBytes)

    // Write plaintext bytes
    val numBytes = 8
    plainTextBytes = HmacUtil.generateRandomBytes(numBytes)
    writeByteArray(plainTextBytes)

    // Write HMAC bytes
    hmacBytes = HmacUtil.hmacValue(plainTextBytes, keyProvider.getKey(indexName))
    writeByteArray(hmacBytes)

    // Return the current file pointer (i.e. header offset)
    raf.getFilePointer
  }

  /**
    * Writes the byte array.
    * @param byteArray data to be written
    */
  private def writeByteArray(byteArray: Array[Byte]) {
    raf.writeInt(byteArray.length)
    raf.write(byteArray)
  }

  /**
    * Reads the file header.
    */
  def readHeader(): Unit = {
    raf.seek(0)

    indexNameBytes = readBytesFromCurrentFilePointer
    plainTextBytes = readBytesFromCurrentFilePointer
    hmacBytes = readBytesFromCurrentFilePointer
  }

  /**
    * Reads the current bytes.
    * @return the data read
    */
  @throws[java.io.IOException]
  private def readBytesFromCurrentFilePointer: Array[Byte] = {
    /* Read the length of the following byte array in the file. */
    val num_bytes: Int = raf.readInt
    val byteArray: Array[Byte] = new Array[Byte](num_bytes)
    raf.readFully(byteArray)
    byteArray
  }

}
