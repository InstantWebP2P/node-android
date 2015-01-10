/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.iwebpp.libuvpp.tests;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.iwebpp.libuvpp.Constants;
import com.iwebpp.libuvpp.Files;
import com.iwebpp.libuvpp.Files.OpenedFile;
import com.iwebpp.libuvpp.Stats;
import com.iwebpp.libuvpp.cb.FileCallback;
import com.iwebpp.libuvpp.cb.FileCloseCallback;
import com.iwebpp.libuvpp.cb.FileOpenCallback;
import com.iwebpp.libuvpp.cb.FileReadCallback;
import com.iwebpp.libuvpp.cb.FileReadDirCallback;
import com.iwebpp.libuvpp.cb.FileWriteCallback;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class FilesTest extends TestBase {

    private final HandleFactory handleFactory = newFactory();

    private String testName;

    public void startSession(final Method method) throws Exception {
        testName = (TMPDIR.endsWith(File.separator) ? TMPDIR : TMPDIR + File.separator) + method.getName();
    }

    public void endSession(final Method method) {
        final Files handle = handleFactory.newFiles();

        cleanupFiles(handle, testName);
        cleanupFiles(handle, testName + ".txt");
        cleanupFiles(handle, testName + "-new.txt");
        cleanupFiles(handle, testName + "2.txt");
    }

    ///@Test
    public void testGetPath() {
        final String filename = testName + ".txt";
        final Files handle = handleFactory.newFiles();

        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        final OpenedFile openedFile = handle.getOpenedFile(fd);
        ///Assert.assertEquals(openedFile.getPath(), filename);
        cleanupFiles(handle, filename);
    }

    ///@Test
    public void testOpenWriteReadAndCloseSync() {
        final String filename = testName + ".txt";
        final ByteBuffer b = ByteBuffer.wrap("some data".getBytes());
        final Files handle = handleFactory.newFiles();

        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        ///Assert.assertTrue(fd >= 0);
        handle.write(fd, b, 0, b.limit(), 0);
        final ByteBuffer bb = ByteBuffer.allocateDirect(b.capacity());
        handle.read(fd, bb, 0, bb.limit(), 0);
        final int status = handle.close(fd);
        ///Assert.assertTrue(status == 0);
        ///Assert.assertEquals(b, bb);
        cleanupFiles(handle, filename);
    }

    ///@Test
    public void testOpenWriteReadAndCloseAsync() throws Throwable {
        final String filename = testName + ".txt";
        final String data = "some data";
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final ByteBuffer writeBuffer = ByteBuffer.wrap(data.getBytes());
        final ByteBuffer readBuffer = ByteBuffer.allocateDirect(writeBuffer.capacity());
        final AtomicInteger fd = new AtomicInteger();
        final AtomicBoolean openCallbackCalled = new AtomicBoolean(false);
        final AtomicBoolean writeCallbackCalled = new AtomicBoolean(false);
        final AtomicBoolean readCallbackCalled = new AtomicBoolean(false);
        final AtomicBoolean closeCallbackCalled = new AtomicBoolean(false);

        handle.setOpenCallback(new FileOpenCallback() {
            @Override
            public void onOpen(final Object context, final int file, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                openCallbackCalled.set(true);
                checkException(error);
                fd.set(file);
                ///Assert.assertTrue(fd.get() > 0);
                handle.write(fd.get(), writeBuffer, 0, writeBuffer.limit(), 0, FilesTest.this);
            }
        });

        handle.setWriteCallback(new FileWriteCallback() {
            @Override
            public void onWrite(Object context, int bytesWritten, Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                writeCallbackCalled.set(true);
                ///Assert.assertNull(error);
                ///Assert.assertEquals(bytesWritten, data.getBytes().length);
                handle.read(fd.get(), readBuffer, 0, readBuffer.limit(), 0, FilesTest.this);
            }
        });

        handle.setReadCallback(new FileReadCallback() {
            @Override
            public void onRead(Object context, int bytesRead, ByteBuffer data, Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                readCallbackCalled.set(true);
                ///Assert.assertNull(error);
                ///Assert.assertEquals(bytesRead, writeBuffer.limit());
                ///Assert.assertEquals(data, writeBuffer);
                handle.close(fd.get(), FilesTest.this);
            }
        });

        handle.setCloseCallback(new FileCloseCallback() {
            @Override
            public void onClose(final Object context, final int file, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                closeCallbackCalled.set(true);
                checkException(error);
                ///Assert.assertEquals(file, fd.get());
                cleanupFiles(handle, filename);
            }
        });

        handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO, FilesTest.this);
        loop.run();
        ///Assert.assertTrue(openCallbackCalled.get());
        ///Assert.assertTrue(writeCallbackCalled.get());
        ///Assert.assertTrue(readCallbackCalled.get());
        ///Assert.assertTrue(closeCallbackCalled.get());
    }

    ///@Test
    public void testUnlinkSync() {
        final String filename = testName + ".txt";
        final Files handle = handleFactory.newFiles();

        @SuppressWarnings("unused")
        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        final int status = handle.unlink(filename);
        ///Assert.assertTrue(status == 0);
    }

    ///@Test
    public void testUnlinkAsync() throws Throwable {
        final String filename = testName + ".txt";
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final AtomicBoolean unlinkCallbackCalled = new AtomicBoolean(false);

        handle.setUnlinkCallback(new FileCallback() {
            @Override
            public void onDone(final Object context, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                unlinkCallbackCalled.set(true);
                checkException(error);
            }
        });

        handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        handle.unlink(filename, FilesTest.this);
        loop.run();
        ///Assert.assertTrue(unlinkCallbackCalled.get());
    }

    ///@Test
    public void testMkdirRmdirSync() {
        final String dirname = testName;
        final Files handle = handleFactory.newFiles();

        int status = handle.mkdir(dirname, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        ///Assert.assertTrue(status == 0);
        status = handle.rmdir(dirname);
        ///Assert.assertTrue(status == 0);
    }

    ///@Test
    public void testMkdirRmdirAsync() throws Throwable {
        final String dirname = testName;
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final AtomicBoolean mkdirCallbackCalled = new AtomicBoolean(false);
        final AtomicBoolean rmdirCallbackCalled = new AtomicBoolean(false);

        handle.setMkDirCallback( new FileCallback() {
            @Override
            public void onDone(final Object context, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                mkdirCallbackCalled.set(true);
                checkException(error);
                handle.rmdir(dirname, FilesTest.this);
            }
        });

        handle.setRmDirCallback(new FileCallback() {
            @Override
            public void onDone(final Object context, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                rmdirCallbackCalled.set(true);
                checkException(error);
            }
        });

        final int status = handle.mkdir(dirname, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO, FilesTest.this);
        ///Assert.assertTrue(status == 0);
        loop.run();
        ///Assert.assertTrue(mkdirCallbackCalled.get());
        ///Assert.assertTrue(rmdirCallbackCalled.get());

    }

    ///@Test
    public void testReaddirSync() {
        final Files handle = handleFactory.newFiles();
        final String filename = "src";
        final String[] names = handle.readdir(filename, Constants.O_RDONLY);
        ///Assert.assertEquals(names.length, 2);
    }

    ///@Test
    public void testReaddirAsync() throws Throwable {
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final String filename = "src";
        final AtomicBoolean readdirCallbackCalled = new AtomicBoolean(false);

        handle.setReadDirCallback(new FileReadDirCallback() {
            @Override
            public void onReadDir(Object context, String[] names, Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                readdirCallbackCalled.set(true);
                ///Assert.assertEquals(names.length, 2);
            }
        });

        final String[] names = handle.readdir(filename, Constants.O_RDONLY, FilesTest.this);
        ///Assert.assertEquals(names, null);
        loop.run();
        ///Assert.assertTrue(readdirCallbackCalled.get());
    }

    ///@Test
    public void testRenameSync() {
        final String filename = testName + ".txt";
        final String newName = testName + "-new" + ".txt";
        final Files handle = handleFactory.newFiles();

        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        handle.close(fd);
        handle.rename(filename, newName);
        ///Assert.assertTrue (handle.open(newName, Constants.O_RDONLY, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO) > 0);
        cleanupFiles(handle, newName);
    }


    ///@Test
    public void testRenameAsync() throws Throwable {
        final String filename = testName + ".txt";
        final String newName = testName + "-new" + ".txt";
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final AtomicBoolean renameCallbackCalled = new AtomicBoolean(false);

        handle.setRenameCallback(new FileCallback() {
            @Override
            public void onDone(final Object context, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                renameCallbackCalled.set(true);
                checkException(error);
                ///Assert.assertTrue (handle.open(newName, Constants.O_RDONLY, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO) > 0);
                cleanupFiles(handle, newName);
            }
        });

        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        handle.close(fd);
        handle.rename(filename, newName, FilesTest.this);
        loop.run();
        ///Assert.assertTrue(renameCallbackCalled.get());
    }

    ///@Test
    public void testFtruncateSync() {
        final String filename = testName + ".txt";
        final Files handle = handleFactory.newFiles();

        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        handle.ftruncate(fd, 1000);
        final Stats stats = handle.fstat(fd);
        ///Assert.assertEquals(stats.getSize(), 1000);
        cleanupFiles(handle, filename);
    }

    ///@Test
    public void testFtruncateAsync() throws Throwable {
        final String filename = testName + ".txt";
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final AtomicInteger fd = new AtomicInteger();
        final AtomicBoolean ftruncateCallbackCalled = new AtomicBoolean(false);

        handle.setFTruncateCallback(new FileCallback() {
            @Override
            public void onDone(final Object context, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                ftruncateCallbackCalled.set(true);
                checkException(error);
                final Stats stats = handle.fstat(fd.get());
                ///Assert.assertEquals(stats.getSize(), 1000);
                cleanupFiles(handle, filename);
            }
        });

        fd.set(handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO));
        handle.ftruncate(fd.get(), 1000, FilesTest.this);
        loop.run();
        ///Assert.assertTrue(ftruncateCallbackCalled.get());
    }

    ///@Test
    public void testLinkSync() {
        final String filename = testName + ".txt";
        final String filename2 = testName + "2.txt";
        final Files handle = handleFactory.newFiles();

        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        final ByteBuffer b = ByteBuffer.wrap("some data".getBytes());
        handle.write(fd, b, 0, b.limit(), 0);
        handle.close(fd);
        handle.link(filename, filename2);
        final Stats stats = handle.stat(filename2);
        ///Assert.assertEquals(stats.getSize(), b.limit());
        cleanupFiles(handle, filename, filename2);
    }

    ///@Test
    public void testLinkAsync() throws Throwable {
        final String filename = testName + ".txt";
        final String filename2 = testName + "2.txt";
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final AtomicBoolean linkCallbackCalled = new AtomicBoolean();
        final ByteBuffer b = ByteBuffer.wrap("some data".getBytes());

        handle.setLinkCallback(new FileCallback() {
            @Override
            public void onDone(final Object context, final Exception error) throws Exception {
                ///Assert.assertEquals(context, FilesTest.this);
                linkCallbackCalled.set(true);
                final Stats stats = handle.stat(filename2);
                ///Assert.assertEquals(stats.getSize(), b.limit());
                cleanupFiles(handle, filename, filename2);
            }
        });

        final int fd = handle.open(filename, Constants.O_RDWR | Constants.O_CREAT, Constants.S_IRWXU | Constants.S_IRWXG | Constants.S_IRWXO);
        handle.write(fd, b, 0, b.limit(), 0);
        handle.close(fd);
        handle.link(filename, filename2, FilesTest.this);
        loop.run();
        ///Assert.assertTrue(linkCallbackCalled.get());
    }

    private void cleanupFiles(final Files handle, final String... files) {
        for (int i = 0; i < files.length; i++) {
            try {
                final String test = files[i];
                final Stats stat = handle.stat(test);
                if ((stat.getMode() & Constants.S_IFMT) == Constants.S_IFDIR) {
                    handle.rmdir(test);
                } else if ((stat.getMode() & Constants.S_IFMT) == Constants.S_IFREG) {
                    handle.unlink(test);
                }
            } catch (final Exception ignore) {
            }
        }
    }

    private void checkException(final Exception error) {
        if (error != null) {
            throw new RuntimeException(error);
        }
    }

    public static void main(String[] args) throws Exception {
        final String[] classes = {FilesTest.class.getName()};
        ///TestRunner.main(classes);
    }
}
