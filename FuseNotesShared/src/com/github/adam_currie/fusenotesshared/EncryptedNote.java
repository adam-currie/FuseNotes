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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 *
 * @author Adam Currie
 */
public class EncryptedNote{
    private String userId;
    private long noteId;
    private Timestamp createDate;
    private Timestamp editDate;
    private String encryptedBody;
    private String signature;
    private boolean isDeleted;
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    //todo: maybe check signature stuff in constructor, and logical checks
    public EncryptedNote(Timestamp createDate, Timestamp editDate, boolean isDeleted, String encryptedBody, long noteId, String userId, String signature){
        this.createDate = createDate;
        this.editDate = editDate;
        this.isDeleted = isDeleted;
        this.encryptedBody = encryptedBody;
        this.noteId = noteId;
        this.userId = userId;
        this.signature = signature;
    }
    
}
