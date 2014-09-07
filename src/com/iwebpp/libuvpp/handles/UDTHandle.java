// UDT wrapper by tom zhou <iwebpp@gmail.com>

package com.iwebpp.libuvpp.handles;

import java.util.Objects;

import com.iwebpp.libuvpp.Address;
import com.iwebpp.libuvpp.LibUVPermission;
import com.iwebpp.libuvpp.LibUVPermission.AddressResolver;

public class UDTHandle extends StreamHandle {

    private int bindPort = 0;
	public Object owner;
	public boolean reading;

    public UDTHandle(final LoopHandle loop) {
        super(_new(loop.pointer()), loop);
    }

    public UDTHandle(final LoopHandle loop, final long socket) {
        super(_new(loop.pointer(), socket), loop);
    }

    public UDTHandle(final LoopHandle loop, final long pointer, boolean dummy) {
        super(pointer, loop);
    }

    public int bind(final String address, final int port) {
        Objects.requireNonNull(address);
        bindPort = port;
        LibUVPermission.checkBind(address, port);
        return _bind(pointer, address, port);
    }

    public int bind6(final String address, final int port) {
        Objects.requireNonNull(address);
        bindPort = port;
        LibUVPermission.checkBind(address, port);
        return _bind6(pointer, address, port);
    }

    public int connect(final String address, final int port) {
        Objects.requireNonNull(address);
        LibUVPermission.checkConnect(address, port);
        return _connect(pointer, address, port, loop.getContext());
    }

    public int connect6(final String address, final int port) {
        Objects.requireNonNull(address);
        LibUVPermission.checkConnect(address, port);
        return _connect6(pointer, address, port, loop.getContext());
    }

    @Override
    public int listen(final int backlog) {
        LibUVPermission.checkListen(bindPort);
        return super.listen(backlog);
    }

    @Override
    public int accept(final StreamHandle client) {
        Objects.requireNonNull(client);
        assert client instanceof UDTHandle;
        final UDTHandle udtClient = (UDTHandle) client;
        final int accepted = super.accept(client);
        // Check once the native call has been done otherwise peerName is not available.
        // If Accept becomes asynchronous, we will have to adapt the check to be done once
        // the peerName is available.
        LibUVPermission.checkAccept(new AddressResolver() {
            @Override
            public Address resolve() {
                return udtClient.getPeerName();
            }
        });
        return accepted;
    }

    public Address getSocketName() {
        return _socket_name(pointer);
    }

    public Address getPeerName() {
        return _peer_name(pointer);
    }

    public int open(final long socket) {
        return _open(pointer, socket);
    }

    public int setNoDelay(final boolean enable) {
        return _no_delay(pointer, enable ? 1 : 0);
    }

    public int setKeepAlive(final boolean enable,
                            final int delay) {
        return _keep_alive(pointer, enable ? 1 : 0, delay);
    }

    public int setSimultaneousAccepts(final boolean enable) {
        return _simultaneous_accepts(pointer, enable ? 1 : 0);
    }

    private static native long _new(final long loop);

    private static native long _new(final long loop, final long socket);

    private native int _bind(final long ptr, final String address, final int port);

    private native int _bind6(final long ptr, final String address, final int port);

    private native int _connect(final long ptr, final String address, final int port, final Object context);

    private native int _connect6(final long ptr, final String address, final int port, final Object context);

    private native int _open(final long ptr, final long socket);

    private native Address _socket_name(final long ptr);

    private native Address _peer_name(final long ptr);

    private native int _no_delay(final long ptr, final int enable);

    private native int _keep_alive(final long ptr, final int enable, final int delay);

    private native int _simultaneous_accepts(final long ptr, final int enable);

}
