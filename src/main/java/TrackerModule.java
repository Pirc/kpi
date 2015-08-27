package pirc.modules;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.AbstractModule;

import akka.actor.ActorSystem;

import pirc.kpi.TrackerApi;
import pirc.kpi.TrackerApiImpl;

public class TrackerModule 
extends AbstractModule 
implements Provider<TrackerApi> 
{
    @Inject ActorSystem system;

    protected void configure() {
        bind(pirc.kpi.TrackerApi.class).toProvider(this.getClass());
    }

    public TrackerApi get() {
        return new TrackerApiImpl(system);
    }
}
