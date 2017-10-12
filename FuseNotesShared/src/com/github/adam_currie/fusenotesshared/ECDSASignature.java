/*
 * The MIT License
 *
 * Copyright 2017 Adam Currie.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adam_currie.fusenotesshared;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 *
 * @author Adam Currie
 */
public class ECDSASignature implements Serializable{
    private final BigInteger[] rs;

    ECDSASignature(BigInteger[] rs){
        this.rs = rs.clone();
    }

    BigInteger r(){
        return rs[0];
    }
    
    BigInteger s(){
        return rs[1];
    }

    public byte[] toBytes(){
        byte[] bytes = new byte[66];
        
        byte[] r = rs[0].toByteArray();
        byte[] s = rs[1].toByteArray();
        
        //in the binary representation of a negative BigInteger leading 1's are inconsequential, and for positive values leading zeros are inconsequential
        
        //copy with leading zeros
        System.arraycopy(r, 0, bytes, 33-r.length, r.length);
        //if the integer is negative, pad with leading 1's (-1 as a byte is 1111111 in binary)
        if(r[0] < 0){
            java.util.Arrays.fill(bytes, 0, 33-r.length, (byte)-1);
        }
        
        //copy with leading zeros
        System.arraycopy(s, 0, bytes, 66-s.length, s.length);
        //if the integer is negative, pad with leading 1's (-1 as a byte is 1111111 in binary)
        if(s[0] < 0){
            java.util.Arrays.fill(bytes, 33, 66-s.length, (byte)-1);
        }
        
        return bytes;
    }
    
    public static ECDSASignature fromBytes(byte[] bytes){
        BigInteger[] rs = new BigInteger[]{
            new BigInteger(Arrays.copyOfRange(bytes, 0, 33)), 
            new BigInteger(Arrays.copyOfRange(bytes, 33, 66))
        };
        
        return new ECDSASignature(rs);
    }
}