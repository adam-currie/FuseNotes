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
package com.github.adam_currie.fusenotesclient;

import com.github.adam_currie.fusenotesshared.ECDSASignature;
import com.github.adam_currie.fusenotesshared.ECDSASignerVerifier;
import com.github.adam_currie.fusenotesshared.ECDSAUtil;
import com.github.adam_currie.fusenotesshared.EncryptedNote;
import com.github.adam_currie.fusenotesshared.NoteID;
import com.github.adam_currie.fusenotesshared.Protocol;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam Currie
 */
public class NoteStore extends NoteContainer implements Closeable, NoteListener{

    static{
        try{
            Class.forName("org.sqlite.JDBC");
        }catch(ClassNotFoundException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    private static final String LAST_SERVER_SYNC_PATH = "last_server_sync.sav";
    private NoteStoreListener storeListener;
    private final String URL_STR = "http://localhost:8080/FuseNotesServer/NotesServlet";//todo
    private URL url;
    private ScheduledExecutorService ses = Executors.newScheduledThreadPool(4);//todo: test performance of different poolsizes
    private ConnectionToServer server;
    
    /**
     * 
     * @param privateKeyStr
     * @param syncIntertvalSeconds      will be set to MIN_SYNC_INTERVAL_SECONDS if lower
     * @param storeListener
     * @throws SQLException
     * @throws InvalidKeyException 
     */
    public NoteStore(String privateKeyStr, int syncIntertvalSeconds, NoteStoreListener storeListener) throws SQLException, InvalidKeyException{
        super(  
                new NoteFactory(
                    new ECDSASignerVerifier(ECDSAUtil.toPrivateKeyParams(privateKeyStr)),
                    new AESEncryption(privateKeyStr)
                ),
                true
        );
        
        if(syncIntertvalSeconds < Protocol.MIN_SYNC_INTERVAL_SECONDS){
            syncIntertvalSeconds = Protocol.MIN_SYNC_INTERVAL_SECONDS;
        }

        this.storeListener = storeListener;

        try{
            url = new URL(URL_STR);
        }catch(MalformedURLException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        //LOAD NOTES
        
        ArrayList<Note> notesFromDB = LocalDB.getAllNotes(noteFactory);
        
        notes.addAll(notesFromDB);
        storeListener.notesLoaded(new SkipDeletedNotesIterator(notesFromDB.iterator()));
        
        try{
            server = new ConnectionToServer(URL_STR, noteFactory);
        }catch(MalformedURLException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        //todo
        notes.stream().map(n -> n.getEncryptedNote());//debug
        for(Note note : notes){
            //todo
        }
        
        server.startAutoUpdate(syncIntertvalSeconds, 0);
    }
    
    @Override
    public Note addNote(){
        //todo: update db and stuff
        Note n = super.addNote();
        n.setNoteListener(this);
        return n;
    }


    
    /*
     * Method               generateKeyPair
     * Description          generates a private key in base64
     * Returns
     *  String              generated key
     */
    public static String generateKey(){
        return ECDSAUtil.generatePrivateKeyStr();
    }

    /*
     * Method checkKeyValid Description checks the validity of a key Params
     * String key private key in base 64 Returns Boolean whether the key is a
     * valid ecdsa key
     */
    public static boolean checkKeyValid(String keyStr){
        return ECDSAUtil.checkKeyValid(keyStr);
    }

    /**
     * Method createNote Description adds a new note
     *
     * @param waitForEdit whether or not to wait for an edit before saving this
     * note
     * @return the new note
     */
    public Note createNote(boolean waitForEdit){
        Note note = noteFactory.createNote();
        
        ses.execute(() -> {
            notes.add(note);
            
            if(!waitForEdit){
                try{
                    LocalDB.addOrUpdate(note);
                }catch(SQLException ex){
                    Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
                    //todo retry
                }
                
                //todo: send to server and stuff
            }
        });

        return note;
    }

    public String getPrivateKey(){
        return noteFactory.getSigner().getPrivateKeyString();
    }

    /**
     * Shuts down the instance and waits for all threads to stop.
     * @throws IOException 
     */
    @Override
    public void close() throws IOException{
        shutdown();
        try{
            ses.awaitTermination(10, TimeUnit.SECONDS);
        }catch(InterruptedException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            //todo
        }
    }

    /**
     * Shuts down the instance without blocking.
     */
    public void shutdown(){
        ses.shutdown();
    }

    @Override
    public void noteChanged(Note note, EncryptedNote subNote){
        try{
            LocalDB.addOrUpdate(noteFactory.createNote(subNote));//todo: maybe change this, like the createNote(EncryptedNote) method will probably need the other fields of note like the upcoming syncedwithserver flag
        }catch(SQLException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            //todo retry
        }
    }

    private static class SkipDeletedNotesIterator implements Iterator<Note>{
        private Iterator<Note> iterator;
        private Note next = null;
        
        public SkipDeletedNotesIterator(Iterator<Note> iterator){
            this.iterator = iterator;
            advanceNext();
        }

        @Override
        public boolean hasNext(){
            return next != null;
        }

        @Override
        public Note next(){
            if(next == null){
                throw new NoSuchElementException();
            }
            
            Note temp = next;
            advanceNext();
            return temp;
        }

        private void advanceNext(){
            do{
                if(iterator.hasNext()){
                    next = iterator.next();
                }else{
                    next = null;
                }
            }while(next != null && next.getDeleted());//skip deleted ones
        }
    }   

}
