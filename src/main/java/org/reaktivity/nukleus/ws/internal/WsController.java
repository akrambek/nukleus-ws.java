/**
 * Copyright 2016-2018 The Reaktivity Project
 *
 * The Reaktivity Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.reaktivity.nukleus.ws.internal;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

import java.util.concurrent.CompletableFuture;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.reaktivity.nukleus.Controller;
import org.reaktivity.nukleus.ControllerSpi;
import org.reaktivity.nukleus.ws.internal.types.Flyweight;
import org.reaktivity.nukleus.ws.internal.types.control.FreezeFW;
import org.reaktivity.nukleus.ws.internal.types.control.Role;
import org.reaktivity.nukleus.ws.internal.types.control.RouteFW;
import org.reaktivity.nukleus.ws.internal.types.control.UnrouteFW;
import org.reaktivity.nukleus.ws.internal.types.control.WsRouteExFW;

public final class WsController implements Controller
{
    private static final int MAX_SEND_LENGTH = 1024; // TODO: Configuration and Context

    // TODO: thread-safe flyweights or command queue from public methods
    private final RouteFW.Builder routeRW = new RouteFW.Builder();
    private final UnrouteFW.Builder unrouteRW = new UnrouteFW.Builder();
    private final FreezeFW.Builder freezeRW = new FreezeFW.Builder();

    private final WsRouteExFW.Builder routeExRW = new WsRouteExFW.Builder();

    private final ControllerSpi controllerSpi;
    private final MutableDirectBuffer writeBuffer;

    public WsController(
        ControllerSpi controllerSpi)
    {
        this.controllerSpi = controllerSpi;
        this.writeBuffer = new UnsafeBuffer(allocateDirect(MAX_SEND_LENGTH).order(nativeOrder()));
    }

    @Override
    public int process()
    {
        return controllerSpi.doProcess();
    }

    @Override
    public void close() throws Exception
    {
        controllerSpi.doClose();
    }

    @Override
    public Class<WsController> kind()
    {
        return WsController.class;
    }

    @Override
    public String name()
    {
        return WsNukleus.NAME;
    }

    public CompletableFuture<Long> routeServer(
        String localAddress,
        String remoteAddress)
    {
        long correlationId = controllerSpi.nextCorrelationId();

        RouteFW route = routeRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .correlationId(correlationId)
                .nukleus(name())
                .role(b -> b.set(Role.SERVER))
                .localAddress(localAddress)
                .remoteAddress(remoteAddress)
                .build();

        return controllerSpi.doRoute(route.typeId(), route.buffer(), route.offset(), route.sizeof());
    }

    public CompletableFuture<Long> routeServer(
        String localAddress,
        String remoteAddress,
        String protocol,
        String scheme,
        String authority,
        String path)
    {
        long correlationId = controllerSpi.nextCorrelationId();

        RouteFW route = routeRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .correlationId(correlationId)
                .nukleus(name())
                .role(b -> b.set(Role.SERVER))
                .localAddress(localAddress)
                .remoteAddress(remoteAddress)
                .extension(b -> b.set(visitRouteEx(protocol, scheme, authority, path)))
                .build();

        return controllerSpi.doRoute(route.typeId(), route.buffer(), route.offset(), route.sizeof());
    }

    public CompletableFuture<Long> routeClient(
        String localAddress,
        String remoteAddress)
    {
        long correlationId = controllerSpi.nextCorrelationId();

        RouteFW route = routeRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .correlationId(correlationId)
                .nukleus(name())
                .role(b -> b.set(Role.CLIENT))
                .localAddress(localAddress)
                .remoteAddress(remoteAddress)
                .build();

        return controllerSpi.doRoute(route.typeId(), route.buffer(), route.offset(), route.sizeof());
    }

    public CompletableFuture<Long> routeClient(
        String localAddress,
        String remoteAddress,
        String protocol,
        String scheme,
        String authority,
        String path)
    {
        long correlationId = controllerSpi.nextCorrelationId();

        RouteFW route = routeRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                .correlationId(correlationId)
                .nukleus(name())
                .role(b -> b.set(Role.CLIENT))
                .localAddress(localAddress)
                .remoteAddress(remoteAddress)
                .extension(b -> b.set(visitRouteEx(protocol, scheme, authority, path)))
                .build();

        return controllerSpi.doRoute(route.typeId(), route.buffer(), route.offset(), route.sizeof());
    }

    public CompletableFuture<Void> unroute(
        long routeId)
    {
        long correlationId = controllerSpi.nextCorrelationId();

        UnrouteFW unroute = unrouteRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                                     .correlationId(correlationId)
                                     .nukleus(name())
                                     .routeId(routeId)
                                     .build();

        return controllerSpi.doUnroute(unroute.typeId(), unroute.buffer(), unroute.offset(), unroute.sizeof());
    }

    public CompletableFuture<Void> freeze()
    {
        long correlationId = controllerSpi.nextCorrelationId();

        FreezeFW freeze = freezeRW.wrap(writeBuffer, 0, writeBuffer.capacity())
                                  .correlationId(correlationId)
                                  .nukleus(name())
                                  .build();

        return controllerSpi.doFreeze(freeze.typeId(), freeze.buffer(), freeze.offset(), freeze.sizeof());
    }

    private Flyweight.Builder.Visitor visitRouteEx(
        String protocol,
        String scheme,
        String authority,
        String path)
    {
        return (buffer, offset, limit) ->
            routeExRW.wrap(buffer, offset, limit)
                     .protocol(protocol)
                     .scheme(scheme)
                     .authority(authority)
                     .path(path)
                     .build()
                     .sizeof();
    }
}
