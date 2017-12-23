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

import com.github.adam_currie.fusenotesshared.ECDSASignerVerifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam Currie
 */
class ConnectionToServer{
    private final File saveFile;
    private final URL url;
    private Timestamp lastSync;
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    
    private final Lock autoUpdateLock = new ReentrantLock();
    private ScheduledFuture<?> autoUpdateFuture = null;
    
    ConnectionToServer(String urlStr, NoteFactory noteFactory) throws MalformedURLException{
        url = new URL(urlStr);
        saveFile = new File(url.getHost() + ".sav");
        loadLastSync();
    }
    
    /**
     * starts automatically syncing with server.
     * if already syncing, this will reinitialize the interval.
     * thread-safe along with {@link #stopAutoUpdate() stopAutoUpdate}
     * @param syncIntertval number of seconds between updates
     * @param initialDelay  number of seconds before first update
     */
    public void startAutoUpdate(long syncIntertval, long initialDelay){
        autoUpdateLock.lock();
        try{
            if(autoUpdateFuture != null){
                autoUpdateFuture.cancel(false);
            }
            autoUpdateFuture = ses.scheduleWithFixedDelay(new ServerSyncTask(), initialDelay, syncIntertval, TimeUnit.SECONDS);
        }finally{
            autoUpdateLock.unlock();
        }
    }
    
    /**
     * stops automatically syncing with server.
     * does nothing if syncing is not on.
     * thread-safe along with {@link #startAutoUpdate(long syncIntertval, long initialDelay) startAutoUpdate}
     */
    public void stopAutoUpdate(){
        autoUpdateLock.lock();
        try{
            if(autoUpdateFuture != null){
                autoUpdateFuture.cancel(false);
                autoUpdateFuture = null;
            }
        }finally{
            autoUpdateLock.unlock();
        }
    }
    
    //debug
//    private void saveLastSync(String password){
//        try (PrintWriter out = new PrintWriter(KEY_PATH)) {
//            out.println(password);
//        }catch(FileNotFoundException ex){
//            Logger.getLogger(MainJFrame.class.getName()).log(Level.SEVERE, null, ex);
//            System.exit(1);
//        }
//    }

    private void loadLastSync(){
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))){
            lastSync = (Timestamp)ois.readObject();
        }catch(IOException | ClassNotFoundException ex){
            Logger.getLogger(ConnectionToServer.class.getName()).log(Level.SEVERE, null, ex);
            lastSync = null;
        }
    }

    private class ServerSyncTask implements Runnable{
        @Override
        public void run(){
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
}
