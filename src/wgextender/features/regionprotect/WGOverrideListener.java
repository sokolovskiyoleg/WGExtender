package wgextender.features.regionprotect;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import wgextender.WGExtender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class WGOverrideListener implements Listener {

	private final List<Pair<HandlerList, RegisteredListener>> overriddenEvents = new ArrayList<>();

	public void inject() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		for (Method method : getClass().getMethods()) {
			if (method.isAnnotationPresent(EventHandler.class)) {
				Class<?> eventClass = method.getParameterTypes()[0];
				HandlerList hl = (HandlerList) eventClass.getMethod("getHandlerList").invoke(null);
				for (RegisteredListener listener : hl.getRegisteredListeners()) {
					if (listener.getListener().getClass() == getClassToReplace()) {
						overriddenEvents.add(Pair.of(hl, listener));
						hl.unregister(listener);
					}
				}
			}
		}
		Bukkit.getPluginManager().registerEvents(this, WGExtender.getInstance());
	}

	public void uninject() {
		HandlerList.unregisterAll(this);
		for (var pair : overriddenEvents) {
			pair.first().register(pair.second());
		}
		overriddenEvents.clear();
	}

	protected abstract Class<? extends Listener> getClassToReplace();

}
