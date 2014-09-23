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

package com.iwebpp.libuvpp;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.iwebpp.libuvpp.cb.FileCallback;
import com.iwebpp.libuvpp.cb.FileCloseCallback;
import com.iwebpp.libuvpp.cb.FileOpenCallback;
import com.iwebpp.libuvpp.cb.FileReadCallback;
import com.iwebpp.libuvpp.cb.FileReadDirCallback;
import com.iwebpp.libuvpp.cb.FileReadLinkCallback;
import com.iwebpp.libuvpp.cb.FileStatsCallback;
import com.iwebpp.libuvpp.cb.FileUTimeCallback;
import com.iwebpp.libuvpp.cb.FileWriteCallback;
import com.iwebpp.libuvpp.handles.LoopHandle;

public class Files {

    static {
        _static_initialize();
    }

    private static final Object SYNC_MODE = null;

    // must be equal to values in uv.h
    private static final int UV_FS_UNKNOWN   = -1;
    private static final int UV_FS_CUSTOM    = 0;
    private static final int UV_FS_OPEN      = 1;
    private static final int UV_FS_CLOSE     = 2;
    private static final int UV_FS_READ      = 3;
    private static final int UV_FS_WRITE     = 4;
    private static final int UV_FS_SENDFILE  = 5;
    private static final int UV_FS_STAT      = 6;
    private static final int UV_FS_LSTAT     = 7;
    private static final int UV_FS_FSTAT     = 8;
    private static final int UV_FS_FTRUNCATE = 9;
    private static final int UV_FS_UTIME     = 10;
    private static final int UV_FS_FUTIME    = 11;
    private static final int UV_FS_CHMOD     = 12;
    private static final int UV_FS_FCHMOD    = 13;
    private static final int UV_FS_FSYNC     = 14;
    private static final int UV_FS_FDATASYNC = 15;
    private static final int UV_FS_UNLINK    = 16;
    private static final int UV_FS_RMDIR     = 17;
    private static final int UV_FS_MKDIR     = 18;
    private static final int UV_FS_RENAME    = 19;
    private static final int UV_FS_READDIR   = 20;
    private static final int UV_FS_LINK      = 21;
    private static final int UV_FS_SYMLINK   = 22;
    private static final int UV_FS_READLINK  = 23;
    private static final int UV_FS_CHOWN     = 24;
    private static final int UV_FS_FCHOWN    = 25;

    private FileCallback onCustom = null;
    private FileOpenCallback onOpen = null;
    private FileCloseCallback onClose = null;
    private FileReadCallback onRead = null;
    private FileWriteCallback onWrite = null;
    private FileCallback onSendfile = null;
    private FileStatsCallback onStat = null;
    private FileStatsCallback onLStat = null;
    private FileStatsCallback onFStat = null;
    private FileCallback onFTruncate = null;
    private FileUTimeCallback onUTime = null;
    private FileUTimeCallback onFUTime = null;
    private FileCallback onChmod = null;
    private FileCallback onFChmod = null;
    private FileCallback onFSync = null;
    private FileCallback onFDatasync = null;
    private FileCallback onUnlink = null;
    private FileCallback onRmDir = null;
    private FileCallback onMkDir = null;
    private FileCallback onRename = null;
    private FileReadDirCallback onReadDir = null;
    private FileCallback onLink = null;
    private FileCallback onSymLink = null;
    private FileReadLinkCallback onReadLink = null;
    private FileCallback onChown = null;
    private FileCallback onFChown = null;

    private final long pointer;
    private final LoopHandle loop;
    private final Map<Integer, OpenedFile> openedFiles = new HashMap<Integer, OpenedFile>();

    private boolean closed;

    // should be private but used by unit tests.
    public static final class OpenedFile {
        private final int flags;
        private final String path;
        private OpenedFile(final String path, final int flags) {
            this.path = path;
            this.flags = flags;
        }
        int getFlags() { return flags; }
        public String getPath() { return path; }
    }

    protected Files(final LoopHandle loop) {
        LibUVPermission.checkHandle();
        this.pointer = _new();
        assert pointer != 0;
        this.loop = loop;
        _initialize(pointer, loop.pointer());

        openedFiles.put(0, new OpenedFile("stdin", 0));
        openedFiles.put(1, new OpenedFile("stdout", 0));
        openedFiles.put(2, new OpenedFile("stderr", 0));
    }

    public void setCustomCallback(final FileCallback callback) {
        onCustom = callback;
    }

    public void setOpenCallback(final FileOpenCallback callback) {
        onOpen = callback;
    }

    public void setCloseCallback(final FileCloseCallback callback) {
        onClose = callback;
    }

    public void setReadCallback(final FileReadCallback callback) {
        onRead = callback;
    }

    public void setWriteCallback(final FileWriteCallback callback) {
        onWrite = callback;
    }

    public void setSendfileCallback(final FileCallback callback) {
        onSendfile = callback;
    }

    public void setStatCallback(final FileStatsCallback callback) {
        onStat = callback;
    }

    public void setLStatCallback(final FileStatsCallback callback) {
        onLStat = callback;
    }

    public void setFStatCallback(final FileStatsCallback callback) {
        onFStat = callback;
    }

    public void setFTruncateCallback(final FileCallback callback) {
        onFTruncate = callback;
    }

    public void setUTimeCallback(final FileUTimeCallback callback) {
        onUTime = callback;
    }

    public void setFUTimeCallback(final FileUTimeCallback callback) {
        onFUTime = callback;
    }

    public void setChmodCallback(final FileCallback callback) {
        onChmod = callback;
    }

    public void setFChmodCallback(final FileCallback callback) {
        onFChmod = callback;
    }

    public void setFSyncCallback(final FileCallback callback) {
        onFSync = callback;
    }

    public void setFDatasyncCallback(final FileCallback callback) {
        onFDatasync = callback;
    }

    public void setUnlinkCallback(final FileCallback callback) {
        onUnlink = callback;
    }

    public void setRmDirCallback(final FileCallback callback) {
        onRmDir = callback;
    }

    public void setMkDirCallback(final FileCallback callback) {
        onMkDir = callback;
    }

    public void setRenameCallback(final FileCallback callback) {
        onRename = callback;
    }

    public void setReadDirCallback(final FileReadDirCallback callback) {
        onReadDir = callback;
    }

    public void setLinkCallback(final FileCallback callback) {
        onLink = callback;
    }

    public void setSymLinkCallback(final FileCallback callback) {
        onSymLink = callback;
    }

    public void setReadLinkCallback(final FileReadLinkCallback callback) {
        onReadLink = callback;
    }

    public void setChownCallback(final FileCallback callback) {
        onChown = callback;
    }

    public void setFChownCallback(final FileCallback callback) {
        onFChown = callback;
    }

    public void close() {
        if (!closed) {
            openedFiles.clear();
            _close(pointer);
        }
        closed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public int close(final int fd) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "closeSync");
        Objects.requireNonNull(file);
        LibUVPermission.checkOpenFile(file.getPath(), file.getFlags());
        final int r = _close(pointer, fd, SYNC_MODE, loop.getContext());
        if (r != -1) {
            openedFiles.remove(fd);
        }
        return r;
    }

    public int close(final int fd, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callClose(context, -1, newEBADF("close", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        LibUVPermission.checkOpenFile(file.getPath(), file.getFlags());
        final int r = _close(pointer, fd, context, loop.getContext());
        if (r != -1) {
            openedFiles.remove(fd);
        }
        return r;
    }

    public int open(final String path, final int flags, final int mode) {
        Objects.requireNonNull(path);
        LibUVPermission.checkOpenFile(path, flags);
        final int fd = _open(pointer, path, flags, mode, SYNC_MODE, loop.getContext());
        if (fd != -1) {
            openedFiles.put(fd, new OpenedFile(path, flags));
        }
        return fd;
    }

    public int open(final String path, final int flags, final int mode, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkOpenFile(path, flags);
        return _open(pointer, path, flags, mode, context, loop.getContext());
    }

    public int read(final int fd, final ByteBuffer buffer, final long offset, final long length, final long position) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "readSync");
        Objects.requireNonNull(file);
        Objects.requireNonNull(buffer);
        LibUVPermission.checkReadFile(fd, file.getPath());
        return buffer.hasArray() ?
                _read(pointer, fd, buffer, buffer.array(), length, offset, position, SYNC_MODE, loop.getContext()) :
                _read(pointer, fd, buffer, null, length, offset, position, SYNC_MODE, loop.getContext());
    }

    public int read(final int fd, final ByteBuffer buffer, final long offset, final long length, final long position, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callRead(context, -1, buffer, newEBADF("read", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        Objects.requireNonNull(buffer);
        LibUVPermission.checkReadFile(fd, file.getPath());
        return buffer.hasArray() ?
                _read(pointer, fd, buffer, buffer.array(), length, offset, position, context, loop.getContext()) :
                _read(pointer, fd, buffer, null, length, offset, position, context, loop.getContext());
    }

    public int unlink(final String path) {
        Objects.requireNonNull(path);
        LibUVPermission.checkDeleteFile(path);
        return _unlink(pointer, path, SYNC_MODE, loop.getContext());
    }

    public int unlink(final String path, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkDeleteFile(path);
        return _unlink(pointer, path, context, loop.getContext());
    }

    public int write(final int fd, final ByteBuffer buffer, final long offset, final long length, final long position) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "writeSync");
        Objects.requireNonNull(file);
        Objects.requireNonNull(buffer);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        assert(offset < buffer.limit());
        assert(offset + length <= buffer.limit());
        return buffer.hasArray() ?
                _write(pointer, fd, buffer, buffer.array(), length, offset, position, SYNC_MODE, loop.getContext()) :
                _write(pointer, fd, buffer, null, length, offset, position, SYNC_MODE, loop.getContext());
    }

    public int write(final int fd, final ByteBuffer buffer, final long offset, final long length, final long position, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callWrite(context, -1, newEBADF("write", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        Objects.requireNonNull(buffer);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        assert(offset < buffer.limit());
        assert(offset + length <= buffer.limit());
        return buffer.hasArray() ?
                _write(pointer, fd, buffer, buffer.array(), length, offset, position, context, loop.getContext()) :
                _write(pointer, fd, buffer, null, length, offset, position, context, loop.getContext());
    }

    public int mkdir(final String path, final int mode) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _mkdir(pointer, path, mode, SYNC_MODE, loop.getContext());
    }

    public int mkdir(final String path, final int mode, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _mkdir(pointer, path, mode, context, loop.getContext());
    }

    public int rmdir(final String path) {
        Objects.requireNonNull(path);
        LibUVPermission.checkDeleteFile(path);
        return _rmdir(pointer, path, SYNC_MODE, loop.getContext());
    }

    public int rmdir(final String path, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkDeleteFile(path);
        return _rmdir(pointer, path, context, loop.getContext());
    }

    public String[] readdir(final String path, final int flags) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _readdir(pointer, path, flags, SYNC_MODE, loop.getContext());
    }

    public String[] readdir(final String path, final int flags, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _readdir(pointer, path, flags, context, loop.getContext());
    }

    public Stats stat(final String path) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _stat(pointer, path, SYNC_MODE, loop.getContext());
    }

    public Stats stat(final String path, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _stat(pointer, path, context, loop.getContext());
    }

    public Stats fstat(final int fd) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "fstatSync");
        Objects.requireNonNull(file);
        LibUVPermission.checkReadFile(fd, file.getPath());
        return _fstat(pointer, fd, SYNC_MODE, loop.getContext());
    }

    public Stats fstat(final int fd, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callStats(UV_FS_FSTAT, context, null, newEBADF("fstat", fd), loop.getContext());
            return null;
        }
        Objects.requireNonNull(file);
        LibUVPermission.checkReadFile(fd, file.getPath());
        return _fstat(pointer, fd, context, loop.getContext());
    }

    public int rename(final String path, final String newPath) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(newPath);
        LibUVPermission.checkWriteFile(path);
        LibUVPermission.checkWriteFile(newPath);
        return _rename(pointer, path, newPath, SYNC_MODE, loop.getContext());
    }

    public int rename(final String path, final String newPath, final Object context) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(newPath);
        LibUVPermission.checkWriteFile(path);
        LibUVPermission.checkWriteFile(newPath);
        return _rename(pointer, path, newPath, context, loop.getContext());
    }

    public int fsync(final int fd) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "fsyncSync");
        Objects.requireNonNull(file);
        // If a file is open, it can be synced, no security check.
        return _fsync(pointer, fd, SYNC_MODE, loop.getContext());
    }

    public int fsync(final int fd, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callback(UV_FS_FSYNC, context, newEBADF("fsync", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        // If a file is open, it can be synced, no security check.
        return _fsync(pointer, fd, context, loop.getContext());
    }

    public int fdatasync(final int fd) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "fdatasyncSync");
        Objects.requireNonNull(file);
        // If a file is open, it can be synced, no security check.
        return _fdatasync(pointer, fd, SYNC_MODE, loop.getContext());
    }

    public int fdatasync(final int fd, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callback(UV_FS_FDATASYNC, context, newEBADF("fdatasync", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        // If a file is open, it can be synced, no security check.
        return _fdatasync(pointer, fd, context, loop.getContext());
    }

    public int ftruncate(final int fd, final long offset) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "ftruncateSync");
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _ftruncate(pointer, fd, offset, SYNC_MODE, loop.getContext());
    }

    public int ftruncate(final int fd, final long offset, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callback(UV_FS_FTRUNCATE, context, newEBADF("ftruncate", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _ftruncate(pointer, fd, offset, context, loop.getContext());
    }

    public int sendfile(final int outFd, final int inFd, final long offset, final long length) {
        Objects.requireNonNull(getOpenedFile(outFd));
        Objects.requireNonNull(getOpenedFile(inFd));
        // No security check required.
        return _sendfile(pointer, outFd, inFd, offset, length, SYNC_MODE, loop.getContext());
    }

    public int sendfile(final int outFd, final int inFd, final long offset, final long length, final Object context) {
        Objects.requireNonNull(getOpenedFile(outFd));
        Objects.requireNonNull(getOpenedFile(inFd));
        // No security check required.
        return _sendfile(pointer, outFd, inFd, offset, length, context, loop.getContext());
    }

    public int chmod(final String path, final int mode) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _chmod(pointer, path, mode, SYNC_MODE, loop.getContext());
    }

    public int chmod(final String path, final int mode, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _chmod(pointer, path, mode, context, loop.getContext());
    }

    public int utime(final String path, final double atime, final double mtime) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _utime(pointer, path, atime, mtime, SYNC_MODE, loop.getContext());
    }

    public int utime(final String path, final double atime, final double mtime, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _utime(pointer, path, atime, mtime, context, loop.getContext());
    }

    public int futime(final int fd, final double atime, final double mtime) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "futimeSync");
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _futime(pointer, fd, atime, mtime, SYNC_MODE, loop.getContext());
    }

    public int futime(final int fd, final double atime, final double mtime, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callUTime(UV_FS_FUTIME, context, -1, newEBADF("futime", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _futime(pointer, fd, atime, mtime, context, loop.getContext());
    }

    public Stats lstat(final String path) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _lstat(pointer, path, SYNC_MODE, loop.getContext());
    }

    public Stats lstat(final String path, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _lstat(pointer, path, context, loop.getContext());
    }

    public int link(final String path, final String newPath) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(newPath);
        LibUVPermission.checkHardLink(path, newPath);
        return _link(pointer, path, newPath, SYNC_MODE, loop.getContext());
    }

    public int link(final String path, final String newPath, final Object context) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(newPath);
        LibUVPermission.checkHardLink(path, newPath);
        return _link(pointer, path, newPath, context, loop.getContext());
    }

    public int symlink(final String path, final String newPath, final int flags) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(newPath);
        LibUVPermission.checkSymbolicLink(path, newPath);
        return _symlink(pointer, path, newPath, flags, SYNC_MODE, loop.getContext());
    }

    public int symlink(final String path, final String newPath, final int flags, final Object context) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(newPath);
        LibUVPermission.checkSymbolicLink(path, newPath);
        return _symlink(pointer, path, newPath, flags, context, loop.getContext());
    }

    public String readlink(final String path) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _readlink(pointer, path, SYNC_MODE, loop.getContext());
    }

    public String readlink(final String path, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkReadFile(path);
        return _readlink(pointer, path, context, loop.getContext());
    }

    public int fchmod(final int fd, final int mode) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "fchmodSync");
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _fchmod(pointer, fd, mode, SYNC_MODE, loop.getContext());
    }

    public int fchmod(final int fd, final int mode, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callback(UV_FS_FCHMOD, context, newEBADF("fchmod", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _fchmod(pointer, fd, mode, context, loop.getContext());
    }

    public int chown(final String path, final int uid, final int gid) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _chown(pointer, path, uid, gid, SYNC_MODE, loop.getContext());
    }

    public int chown(final String path, final int uid, final int gid, final Object context) {
        Objects.requireNonNull(path);
        LibUVPermission.checkWriteFile(path);
        return _chown(pointer, path, uid, gid, context, loop.getContext());
    }

    public int fchown(final int fd, final int uid, final int gid) {
        final OpenedFile file = getOpenedFileAssertNonNull(fd, "fchown");
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _fchown(pointer, fd, uid, gid, SYNC_MODE, loop.getContext());
    }

    public int fchown(final int fd, final int uid, final int gid, final Object context) {
        final OpenedFile file = getOpenedFile(fd);
        if (file == null) {
            callback(UV_FS_FCHOWN, context, newEBADF("fchown", fd), loop.getContext());
            return -1;
        }
        Objects.requireNonNull(file);
        LibUVPermission.checkWriteFile(fd, file.getPath());
        return _fchown(pointer, fd, uid, gid, context, loop.getContext());
    }

    // should be private but used by unit tests.
    public OpenedFile getOpenedFile(final int fd) {
        // No security check, can retrieve path of an opened fd.
        return openedFiles.get(fd);
    }

    private OpenedFile getOpenedFileAssertNonNull(final int fd, final String method) {
        final OpenedFile file = openedFiles.get(fd);
        if (file == null) {
            throw newEBADF(method, fd);
        }
        return file;
    }

    private NativeException newEBADF(final String method, final int fd) {
        return new NativeException(9, "EBADF", "Bad file number: " + fd, method, null, null);
    }

    private void callback(final int type, final Object callback, final Exception error,final Object context) {
        switch (type) {
            case UV_FS_CUSTOM:
                if (onCustom != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onCustom, callback, error);
                }
                break;
            case UV_FS_SENDFILE:
                if (onSendfile != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onSendfile, callback, error);
                }
                break;
            case UV_FS_FTRUNCATE:
                if (onFTruncate != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onFTruncate, callback, error);
                }
                break;
            case UV_FS_CHMOD:
                if (onChmod != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onChmod, callback, error);
                }
                break;
            case UV_FS_FCHMOD:
                if (onFChmod != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onFChmod, callback, error);
                }
                break;
            case UV_FS_FSYNC:
                if (onFSync != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onFSync, callback, error);
                }
                break;
            case UV_FS_FDATASYNC:
                if (onFDatasync != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onFDatasync, callback, error);
                }
                break;
            case UV_FS_UNLINK:
                if (onUnlink != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onUnlink, callback, error);
                }
                break;
            case UV_FS_RMDIR:
                if (onRmDir != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onRmDir, callback, error);
                }
                break;
            case UV_FS_MKDIR:
                if (onMkDir != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onMkDir, callback, error);
                }
                break;
            case UV_FS_RENAME:
                if (onRename != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onRename, callback, error);
                }
                break;
            case UV_FS_LINK:
                if (onLink != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onLink, callback, error);
                }
                break;
            case UV_FS_SYMLINK:
                if (onSymLink != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onSymLink, callback, error);
                }
                break;
            case UV_FS_CHOWN:
                if (onChown != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onChown, callback, error);
                }
                break;
            case UV_FS_FCHOWN:
                if (onFChown != null) {
                    loop.getCallbackHandler(context).handleFileCallback(onFChown, callback, error);
                }
                break;
            default: assert false : "unsupported callback type " + type;
        }
    }

    private void callClose(final Object callback, final int fd, final Exception error, final Object context) {
        if (onClose != null) {
            loop.getCallbackHandler(context).handleFileCloseCallback(onClose, callback, fd, error);
        }
    }

    private void callOpen(final Object callback, final int fd, final String path, final int flags, final Exception error, final Object context) {
        if (fd != -1) {
            openedFiles.put(fd, new OpenedFile(path, flags));
        }
        if (onOpen != null) {
            loop.getCallbackHandler(context).handleFileOpenCallback(onOpen, callback, fd, error);
        }
    }

    private void callRead(final Object callback, final int bytesRead, final ByteBuffer data, final Exception error, final Object context) {
        if (onRead != null) {
            loop.getCallbackHandler(context).handleFileReadCallback(onRead, callback, bytesRead, data, error);
        }
    }

    private void callReadDir(final Object callback, final String[] names, final Exception error, final Object context) {
        if (onReadDir != null) {
            loop.getCallbackHandler(context).handleFileReadDirCallback(onReadDir, callback, names, error);
        }
    }

    private void callReadLink(final Object callback, final String name, final Exception error, final Object context) {
        if (onReadLink != null) {
            loop.getCallbackHandler(context).handleFileReadLinkCallback(onReadLink, callback, name, error);
        }
    }

    private void callStats(final int type, final Object callback, final Stats stats, final Exception error, final Object context) {
        switch(type) {
            case UV_FS_FSTAT:
                if (onFStat != null) {
                    loop.getCallbackHandler(context).handleFileStatsCallback(onFStat, callback, stats, error);
                }
                break;
            case UV_FS_LSTAT:
                if (onLStat != null) {
                    loop.getCallbackHandler(context).handleFileStatsCallback(onLStat, callback, stats, error);
                }
                break;
            case UV_FS_STAT:
                if (onStat != null) {
                    loop.getCallbackHandler(context).handleFileStatsCallback(onStat, callback, stats, error);
                }
                break;
            default: assert false : "unsupported callback type " + type;
        }
    }

    private void callUTime(final int type, final Object callback, final long time, final Exception error, final Object context) {
        switch(type) {
            case UV_FS_UTIME:
                if (onUTime != null) {
                    loop.getCallbackHandler(context).handleFileUTimeCallback(onUTime, callback, time, error);
                }
                break;
            case UV_FS_FUTIME:
                if (onFUTime != null) {
                    loop.getCallbackHandler(context).handleFileUTimeCallback(onFUTime, callback, time, error);
                }
                break;
            default: assert false : "unsupported callback type " + type;
        }
    }

    private void callWrite(final Object callback, final int bytesWritten, final Exception error, final Object context) {
        if (onWrite != null) {
            loop.getCallbackHandler(context).handleFileWriteCallback(onWrite, callback, bytesWritten, error);
        }
    }

    private static native void _static_initialize();

    private static native long _new();

    private native void _initialize(final long ptr, final long loop);

    private native int _close(final long ptr);

    private native int _close(final long ptr, final int fd, final Object callback, final Object context);

    private native int _open(final long ptr, final String path, final int flags, final int mode, final Object callback, final Object context);

    private native int _read(final long ptr, final int fd, final ByteBuffer buffer, final byte[] data, final long length, final long offset, final long position, final Object callback, final Object context);

    private native int _unlink(final long ptr, final String path, final Object callback, final Object context);

    private native int _write(final long ptr, final int fd, final ByteBuffer buffer, final byte[] data, final long length, final long offset, final long position, final Object callback, final Object context);

    private native int _mkdir(final long ptr, final String path, final int mode, final Object callback, final Object context);

    private native int _rmdir(final long ptr, final String path, final Object callback, final Object context);

    private native String[] _readdir(final long ptr, final String path, final int flags, final Object callback, final Object context);

    private native Stats _stat(final long ptr, final String path, final Object callback, final Object context);

    private native Stats _fstat(final long ptr, final int fd, final Object callback, final Object context);

    private native int _rename(final long ptr, final String path, final String newPath, final Object callback, final Object context);

    private native int _fsync(final long ptr, final int fd, final Object callback, final Object context);

    private native int _fdatasync(final long ptr, final int fd, final Object callback, final Object context);

    private native int _ftruncate(final long ptr, final int fd, final long offset, final Object callback, final Object context);

    private native int _sendfile(final long ptr, final int outFd, final int inFd, final long offset, final long length, final Object callback, final Object context);

    private native int _chmod(final long ptr, final String path, final int mode, final Object callback, final Object context);

    private native int _utime(final long ptr, final String path, final double atime, final double mtime, final Object callback, final Object context);

    private native int _futime(final long ptr, final int fd, final double atime, final double mtime, final Object callback, final Object context);

    private native Stats _lstat(final long ptr, final String path, final Object callback, final Object context);

    private native int _link(final long ptr, final String path, final String newPath, final Object callback, final Object context);

    private native int _symlink(final long ptr, final String path, final String newPath, final int flags, final Object callback, final Object context);

    private native String _readlink(final long ptr, final String path, final Object callback, final Object context);

    private native int _fchmod(final long ptr, final int fd, final int mode, final Object callback, final Object context);

    private native int _chown(final long ptr, final String path, final int uid, final int gid, final Object callback, final Object context);

    private native int _fchown(final long ptr, final int fd, final int uid, final int gid, final Object callback, final Object context);

}
