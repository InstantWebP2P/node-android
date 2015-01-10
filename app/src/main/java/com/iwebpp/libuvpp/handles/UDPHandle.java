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

package com.iwebpp.libuvpp.handles;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.iwebpp.libuvpp.Address;
import com.iwebpp.libuvpp.LibUVPermission;
import com.iwebpp.libuvpp.cb.UDPCloseCallback;
import com.iwebpp.libuvpp.cb.UDPRecvCallback;
import com.iwebpp.libuvpp.cb.UDPSendCallback;

public class UDPHandle extends Handle {

    private boolean closed;

    private UDPRecvCallback onRecv = null;
    private UDPSendCallback onSend = null;
    private UDPCloseCallback onClose = null;

    public enum Membership {
        // must be equal to uv_membership values in uv.h
        LEAVE_GROUP(0),
        JOIN_GROUP(1);

        final int value;

        Membership(final int value) {
            this.value = value;
        }
    }

    static {
        _static_initialize();
    }

    public void setRecvCallback(final UDPRecvCallback callback) {
        onRecv = callback;
    }

    public void setSendCallback(final UDPSendCallback callback) {
        onSend = callback;
    }

    public void setCloseCallback(final UDPCloseCallback callback) {
        onClose = callback;
    }

    public UDPHandle(final LoopHandle loop) {
        super(_new(loop.pointer()), loop);
        this.closed = false;
        _initialize(pointer);
    }

    public UDPHandle(final LoopHandle loop, final long socket) {
        super(_new(loop.pointer(), socket), loop);
        this.closed = false;
        _initialize(pointer);
    }

    public UDPHandle(final LoopHandle loop, final long pointer, boolean dummy) {
        super(pointer, loop);
        this.closed = false;
        _initialize(pointer);
    }

    public void close() {
        if (!closed) {
            _close(pointer);
        }
        closed = true;
    }

    public Address address() {
        return _address(pointer);
    }

    public int bind(final int port, final String address) {
        Objects.requireNonNull(address);
        LibUVPermission.checkUDPBind(address, port);
        return _bind(pointer, port, address);
    }

    public int bind6(final int port, final String address) {
        Objects.requireNonNull(address);
        LibUVPermission.checkUDPBind(address, port);
        return _bind6(pointer, port, address);
    }

    public int send(final String str,
                    final int port,
                    final String host) {
        Objects.requireNonNull(str);
        Objects.requireNonNull(host);
        final byte[] data;
        try {
            data = str.getBytes("utf-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e); // "utf-8" is always supported
        }
        return send(ByteBuffer.wrap(data), 0, data.length, port, host);
    }

    public int send6(final String str,
                     final int port,
                     final String host) {
        Objects.requireNonNull(str);
        Objects.requireNonNull(host);
        final byte[] data;
        try {
            data = str.getBytes("utf-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e); // "utf-8" is always supported
        }
        return send6(ByteBuffer.wrap(data), 0, data.length, port, host);
    }

    public int send(final String str,
                    final String encoding,
                    final int port,
                    final String host) throws UnsupportedEncodingException {
        Objects.requireNonNull(str);
        Objects.requireNonNull(encoding);
        Objects.requireNonNull(host);
        final byte[] data = str.getBytes(encoding);
        return send(ByteBuffer.wrap(data), 0, data.length, port, host);
    }

    public int send6(final String str,
                     final String encoding,
                     final int port,
                     final String host) throws UnsupportedEncodingException {
        Objects.requireNonNull(str);
        Objects.requireNonNull(encoding);
        Objects.requireNonNull(host);
        final byte[] data = str.getBytes(encoding);
        return send6(ByteBuffer.wrap(data), 0, data.length, port, host);
    }

    public int send(final ByteBuffer buffer,
                    final int port,
                    final String host) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(host);
        LibUVPermission.checkUDPSend(host, port);
        return buffer.hasArray() ?
                _send(pointer, buffer, buffer.array(), 0, buffer.capacity(), port, host, loop.getContext()) :
                _send(pointer, buffer, null, 0, buffer.capacity(), port, host, loop.getContext());
    }

    public int send6(final ByteBuffer buffer,
                     final int port,
                     final String host) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(host);
        LibUVPermission.checkUDPSend(host, port);
        return buffer.hasArray() ?
                _send6(pointer, buffer, buffer.array(), 0, buffer.capacity(), port, host, loop.getContext()) :
                _send6(pointer, buffer, null, 0, buffer.capacity(), port, host, loop.getContext());
    }

    public int send(final ByteBuffer buffer,
                    final int offset,
                    final int length,
                    final int port,
                    final String host) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(host);
        LibUVPermission.checkUDPSend(host, port);
        return buffer.hasArray() ?
                _send(pointer, buffer, buffer.array(), offset, length, port, host, loop.getContext()) :
                _send(pointer, buffer, null, offset, length, port, host, loop.getContext());
    }

    public int send6(final ByteBuffer buffer,
                     final int offset,
                     final int length,
                     final int port,
                     final String host) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(host);
        LibUVPermission.checkUDPSend(host, port);
        return buffer.hasArray() ?
                _send6(pointer, buffer, buffer.array(), offset, length, port, host, loop.getContext()) :
                _send6(pointer, buffer, null, offset, length, port, host, loop.getContext());
    }

    public int recvStart() {
        return _recv_start(pointer);
    }

    public int recvStop() {
        return _recv_stop(pointer);
    }

    public int setTTL(final int ttl) {
        return _set_ttl(pointer, ttl);
    }

    public int setMembership(final String multicastAddress,
                             final String interfaceAddress,
                             final Membership membership) {
        return _set_membership(pointer, multicastAddress, interfaceAddress, membership.value);
    }

    public int setMulticastLoop(final boolean on) {
        return _set_multicast_loop(pointer, on ? 1 : 0);
    }

    public int setMulticastTTL(final int ttl) {
        return _set_multicast_ttl(pointer, ttl);
    }

    public int setBroadcast(final boolean on) {
        return _set_broadcast(pointer, on ? 1 : 0);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private void callRecv(final int nread, final ByteBuffer data, final Address address) {
        if (onRecv != null) {
            loop.getCallbackHandler().handleUDPRecvCallback(onRecv, nread, data, address);
        }
    }

    private void callSend(final int status, final Exception error, final Object context) {
        if (onSend != null) {
            loop.getCallbackHandler(context).handleUDPSendCallback(onSend, status, error);
        }
    }

    private void callClose() {
        if (onClose != null) {
            loop.getCallbackHandler().handleUDPCloseCallback(onClose);
        }
    }

    private static native long _new(final long loop);

    private static native long _new(final long loop, final int fd);

    private static native long _new(final long loop, final long socket);

    private static native void _static_initialize();

    private native void _initialize(final long ptr);

    private native Address _address(final long ptr);

    private native int _bind(final long ptr,
                             final int port,
                             final String host);

    private native int _bind6(final long ptr,
                              final int port,
                              final String host);

    private native int _send(final long ptr,
                             final ByteBuffer buffer,
                             final byte[] data,
                             final int offset,
                             final int length,
                             final int port,
                             final String host,
                             final Object context);

    private native int _send6(final long ptr,
                              final ByteBuffer buffer,
                              final byte[] data,
                              final int offset,
                              final int length,
                              final int port,
                              final String host,
                              final Object context);

    private native int _recv_start(final long ptr);

    private native int _recv_stop(final long ptr);

    private native int _set_ttl(long ptr,
                                int ttl);

    private native int _set_membership(final long ptr,
                                       final String multicastAddress,
                                       final String interfaceAddress,
                                       final int membership);

    private native int _set_multicast_loop(long ptr,
                                           int on);

    private native int _set_multicast_ttl(long ptr,
                                          int ttl);

    private native int _set_broadcast(long ptr,
                                      int on);

    private native void _close(final long ptr);

}
