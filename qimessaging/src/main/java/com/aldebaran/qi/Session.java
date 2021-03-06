/*
 * * Copyright (C) 2015 Aldebaran Robotics* See COPYING for the license
 */
package com.aldebaran.qi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that allows using the messaging layer: it is responsible for connecting
 * services together locally or over the network.
 * <p>
 * The list of available services is contained in the {@link ServiceDirectory}
 * which runs on a Session acting as the entry point for other sessions to
 * connect to. Not all Sessions need to have a {@link ServiceDirectory}, they
 * can also be connected to a Session that already has one. When a Session
 * exposes a service, other connected sessions can contact that service. When
 * Sessions connect to each other, they create an undirected graph.
 * <p>
 * This class allows registering and exposing a new {@link QiService} or
 * retrieving one as an {@link AnyObject}.
 */
public class Session {

    static {
        // Loading native C++ libraries.
        EmbeddedTools.loadEmbeddedLibraries();
    }

    public interface ConnectionListener {
        void onConnected();

        void onDisconnected(String reason);
    }

    // Native function
    private native long qiSessionCreate();

    private native void qiSessionDestroy(long pSession);

    private native Object qiSessionConnect(long pSession, String url);

    private native boolean qiSessionIsConnected(long pSession);

    private native Object qiSessionClose(long pSession);

    private native Object service(long pSession, String name);

    private native int registerService(long pSession, String name, AnyObject obj);

    private native Object unregisterService(long pSession, int idx);

    private native void onDisconnected(long pSession, String callback, Object obj);

    private native void addConnectionListener(long pSession, ConnectionListener listener);

    private native void setClientAuthenticatorFactory(long pSession, ClientAuthenticatorFactory factory);

    private native void loadService(long pSession, String name);

    private native Object waitForService(long pointerSession, String serviceName);

    /**
     * List of URL session endpoints.
     *
     * @param pSession  Reference to session in JNI.
     * @param endpoints List to add URL session endpoints.
     */
    private native void endpoints(long pSession, List<String> endpoints);

    // Members
    private final long _session;
    private final boolean _destroy;

    private List<ConnectionListener> listeners;

    /**
     * Create a qimessaging session.
     */
    public Session() {
        _session = qiSessionCreate();
        _destroy = true;
    }

    protected Session(long session) {
        _session = session;
        _destroy = false;
    }

    /**
     * @return true is session is connected, false otherwise
     */
    public boolean isConnected() {
        if (_session == 0)
            return false;

        return qiSessionIsConnected(_session);
    }

    /**
     * Try to connect to given address.
     *
     * @param serviceDirectoryAddress Address to connect to.
     * @throws Exception on error.
     */
    public Future<Void> connect(String serviceDirectoryAddress) {
        return (Future<Void>)(qiSessionConnect(_session, serviceDirectoryAddress));
    }

    /**
     * Ask for remote service to Service Directory.
     *
     * @param name Name of service.
     * @return the AnyObject future
     */
    public Future<AnyObject> service(String name) {
        return (Future<AnyObject>)(service(_session, name));
    }

    /**
     * Close connection to Service Directory
     *
     * @return Future to track/linked the effective close
     */
    public Future<Void> close() {
        return (Future<Void>)(qiSessionClose(this._session));
    }

    /**
     * Called by garbage collector Finalize is overriden to manually delete C++
     * data
     */
    @Override
    protected void finalize() throws Throwable {
        // if (_destroy)
        // Session.qiSessionDestroy(_session);
        super.finalize();
    }

    /**
     * Register service on Service Directory
     *
     * @param name   Name of new service
     * @param object Instance of service
     * @return the id of the service
     */
    public int registerService(String name, AnyObject object) {
        return registerService(_session, name, object);
    }

    /**
     * Unregister service from Service Directory
     *
     * @param idx is return by registerService
     * @return Future to track/linked to the complete finished
     * @see #registerService(String, AnyObject)
     */
    public Future<Void> unregisterService(int idx) {
        return (Future<Void>)(unregisterService(this._session, idx));
    }

    @Deprecated
    public void onDisconnected(String callback, Object object) {
        onDisconnected(_session, callback, object);
    }

    public synchronized void addConnectionListener(ConnectionListener listener) {
        initializeListeners();
        listeners.add(listener);
    }

    public synchronized void removeConnectionListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    private synchronized void fireConnected() {
        for (ConnectionListener listener : listeners) {
            try {
                listener.onConnected();
            } catch (Exception e) {
                // log exceptions from callbacks, bug ignore them
                e.printStackTrace();
            }
        }
    }

    private synchronized void fireDisconnected(String reason) {
        for (ConnectionListener listener : listeners) {
            try {
                listener.onDisconnected(reason);
            } catch (Exception e) {
                // log exceptions from callbacks, bug ignore them
                e.printStackTrace();
            }
        }
    }

    private void initializeListeners() {
        if (listeners != null)
            return;

        listeners = new ArrayList<ConnectionListener>();
        // register only 1 listener to the native part, and dispatch to local
        // listeners
        addConnectionListener(_session, new ConnectionListener() {
            @Override
            public void onDisconnected(String reason) {
                fireDisconnected(reason);
            }

            @Override
            public void onConnected() {
                fireConnected();
            }
        });
    }

    public void setClientAuthenticatorFactory(ClientAuthenticatorFactory factory) {
        setClientAuthenticatorFactory(_session, factory);
    }

    public void setClientAuthenticator(final ClientAuthenticator authenticator) {
        setClientAuthenticatorFactory(new ClientAuthenticatorFactory() {
            @Override
            public ClientAuthenticator newAuthenticator() {
                // always return the same instance
                return authenticator;
            }
        });
    }

    public void loadService(String name) {
        loadService(_session, name);
    }

    /**
     * List of URL session endpoints.<br>
     * The return list is not modifiable.
     *
     * @return List of URL session endpoints.
     */
    public List<String> endpoints() {
        final List<String> urls = new ArrayList<String>();
        this.endpoints(this._session, urls);
        return Collections.unmodifiableList(urls);
    }

    /**
     * Create future for wait a service is ready to use.
     * @param serviceName Service name
     * @return Future to track/link service connection
     */
    public Future<Void> waitForService(String serviceName) {
        return (Future<Void>)(this.waitForService(this._session, serviceName));
    }
}
