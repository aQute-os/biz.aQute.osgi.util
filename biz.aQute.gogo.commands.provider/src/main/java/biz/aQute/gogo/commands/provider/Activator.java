package biz.aQute.gogo.commands.provider;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Function;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;
import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

@org.osgi.annotation.bundle.Capability(
    namespace = "org.apache.felix.gogo",
    name = "command.implementation",
    version = "1.0.0"
)
@org.osgi.annotation.bundle.Requirement(
    effective = "active",
    namespace = "org.apache.felix.gogo",
    name = "runtime.implementation",
    version = "1.0.0"
)
public class Activator implements BundleActivator {

	final Set<Closeable>	closeables	= new HashSet<>();
	BundleContext			context;
	DTOFormatter			formatter	= new DTOFormatter();

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		registerConverter(context);
		register(DTOFramework.class);
		register(Diagnostics.class);
		register(Builtin.class);
		register(Help.class);
		register(Files.class);
		register(Inspect.class);
		register(Core.class);
		register(DS.class);
		register(LoggerAdminCommands.class);
		register(HTTP.class);
		register(JaxRS.class);
	}

	private void registerConverter(BundleContext context) {
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, 10000);
		ServiceRegistration<Converter> registration = context.registerService(Converter.class, new Converter() {

			@Override
			public Object convert(Class<?> type, Object source) throws Exception {

				if (type == Bundle.class) {
					if (source instanceof Number) {
						Bundle b = context.getBundle(((Number) source).longValue());
						if (b != null) {
							return b;
						}
					} else if (source instanceof String) {
						try {
							long bundleId = Long.valueOf((String) source);
							return context.getBundle(bundleId);
						} catch (Exception e) {

						}
						for (Bundle b : context.getBundles()) {
							if (b.getSymbolicName()
								.equals(source))
								return b;
						}
					}

				}
				if (type == File.class && source instanceof String) {
					if (source.equals("~"))
						return IO.home;
					return IO.getFile((String) source);

				}
				return aQute.lib.converter.Converter.cnv(type, source);
			}

			@Override
			public CharSequence format(Object from, int level, Converter backup) throws Exception {
				try {
					if (from instanceof Enumeration) {
						from = Collections.list((Enumeration<?>) from);
					}
					if (from instanceof File) {
						return ((File) from).getPath();
					}
					if (from instanceof Function) {
						if (from.getClass() == Closure.class)
							return " { " + from.toString() + " } ";
						return "a Function";
					}
					CharSequence formatted = formatter.format(from, level, (o, l, f) -> {
						try {
							return backup.format(o, l, null);
						} catch (Exception e) {
							return Objects.toString(o);
						}
					});

					if (formatted != null) {
						return formatted;
					}
					return formatted;
				} catch (Exception e) {
					e.printStackTrace();
					throw Exceptions.duck(e);
				}
			}

		}, properties);
		closeables.add(() -> {
			registration.unregister();
		});
	}

	<T> ServiceRegistration<?> register(Class<T> c) throws Exception {
		try {
			Constructor<T> constructor = c.getConstructor(BundleContext.class, DTOFormatter.class);

			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(CommandProcessor.COMMAND_SCOPE, "aQute");

			Set<String> commands = new TreeSet<>();
			for (Method m : c.getMethods()) {
				Descriptor d = m.getAnnotation(Descriptor.class);

				if (d != null)
					commands.add(m.getName()
						.toLowerCase());
			}
			T service = constructor.newInstance(context, formatter);

			String[] functions = commands.stream()
				.map(name -> name.startsWith("_") ? name.substring(1) : name)
				.toArray(String[]::new);

			properties.put(CommandProcessor.COMMAND_FUNCTION, functions);

			ServiceRegistration<Object> registration = context.registerService(Object.class, service, properties);
			closeables.add(() -> {
				registration.unregister();
				if (service instanceof Closeable) {
					((Closeable) service).close();
				}
			});
			return registration;
		} catch (Throwable e) {
			return null;
			// ignore
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		closeables.forEach(c -> {
			try {
				c.close();
			} catch (Exception e) {
				// ignore
			}
		});
	}

}
