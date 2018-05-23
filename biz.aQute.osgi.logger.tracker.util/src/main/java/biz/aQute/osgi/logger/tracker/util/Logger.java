package biz.aQute.osgi.logger.tracker.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Marker;

public class Logger implements LogService, org.slf4j.Logger {

	private final String		name;
	private final LoggerFactory	loggerFactory;

	Logger(LoggerFactory loggerFactory, String name) {
		this.loggerFactory = loggerFactory;
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void log(int level, String message) {
		loggerFactory.queue.add(l -> l.log(level, name + ":" + message));
	}

	@Override
	public void log(int level, String message, Throwable throwable) {
		loggerFactory.queue.add(l -> l.log(level, name + ":" + message, throwable));
	}

	@Override
	public void log(ServiceReference ref, int level, String msg) {
		loggerFactory.queue.add((l) -> l.log(ref, level, name + ":" + msg));
	}

	@Override
	public void log(ServiceReference ref, int level, String msg, Throwable throwable) {
		loggerFactory.queue.add((l) -> l.log(ref, level, name + ":" + msg, throwable));
	}

	@Override
	public void debug(String msg) {
		log(LogService.LOG_DEBUG, msg);
	}
	
	

	@Override
	public void debug(String msg, Object arg1) {

		log(LogService.LOG_DEBUG, format(msg, arg1));
	}

	@Override
	public void debug(String msg, Object[] args) {
		log(LogService.LOG_DEBUG, format(msg, args));
	}

	@Override
	public void debug(String msg, Throwable throwable) {
		log(LogService.LOG_DEBUG, msg, throwable);
	}

	@Override
	public void debug(Marker marker, String msg) {
		log(LogService.LOG_DEBUG, marker + ":" + msg);
	}

	@Override
	public void debug(String msg, Object arg1, Object arg2) {
		log(LogService.LOG_DEBUG, format(msg, arg1, arg2));
	}

	@Override
	public void debug(Marker marker, String msg, Object arg) {
		log(LogService.LOG_DEBUG, format(marker + ":" + msg, arg));
	}

	@Override
	public void debug(Marker marker, String msg, Object[] args) {
		log(LogService.LOG_DEBUG, format(marker + ":" + msg, args));
	}

	@Override
	public void debug(Marker marker, String msg, Throwable throwable) {
		log(LogService.LOG_DEBUG, format(marker + ":" + msg), throwable);
	}

	@Override
	public void debug(Marker marker, String msg, Object arg1, Object arg2) {
		log(LogService.LOG_DEBUG, format(marker + ":" + msg, arg1, arg2));
	}

	@Override
	public void error(String msg) {
		log(LogService.LOG_ERROR, msg);
	}
	
	

	@Override
	public void error(String msg, Object arg1) {

		log(LogService.LOG_ERROR, format(msg, arg1));
	}

	@Override
	public void error(String msg, Object[] args) {
		log(LogService.LOG_ERROR, format(msg, args));
	}

	@Override
	public void error(String msg, Throwable throwable) {
		log(LogService.LOG_ERROR, msg, throwable);
	}

	@Override
	public void error(Marker marker, String msg) {
		log(LogService.LOG_ERROR, marker + ":" + msg);
	}

	@Override
	public void error(String msg, Object arg1, Object arg2) {
		log(LogService.LOG_ERROR, format(msg, arg1, arg2));
	}

	@Override
	public void error(Marker marker, String msg, Object arg) {
		log(LogService.LOG_ERROR, format(marker + ":" + msg, arg));
	}

	@Override
	public void error(Marker marker, String msg, Object[] args) {
		log(LogService.LOG_ERROR, format(marker + ":" + msg, args));
	}

	@Override
	public void error(Marker marker, String msg, Throwable throwable) {
		log(LogService.LOG_ERROR, format(marker + ":" + msg), throwable);
	}

	@Override
	public void error(Marker marker, String msg, Object arg1, Object arg2) {
		log(LogService.LOG_ERROR, format(marker + ":" + msg, arg1, arg2));
	}

	@Override
	public void warn(String msg) {
		log(LogService.LOG_ERROR, msg);
	}

    @Override
    public void warn(String msg, Object arg1) {

        log(LogService.LOG_WARNING, format(msg, arg1));
    }

    @Override
    public void warn(String msg, Object[] args) {
        log(LogService.LOG_WARNING, format(msg, args));
    }

    @Override
    public void warn(String msg, Throwable throwable) {
        log(LogService.LOG_WARNING, msg, throwable);
    }

    @Override
    public void warn(Marker marker, String msg) {
        log(LogService.LOG_WARNING, marker + ":" + msg);
    }

    @Override
    public void warn(String msg, Object arg1, Object arg2) {
        log(LogService.LOG_WARNING, format(msg, arg1, arg2));
    }

    @Override
    public void warn(Marker marker, String msg, Object arg) {
        log(LogService.LOG_WARNING, format(marker + ":" + msg, arg));
    }

    @Override
    public void warn(Marker marker, String msg, Object[] args) {
        log(LogService.LOG_WARNING, format(marker + ":" + msg, args));
    }

    @Override
    public void warn(Marker marker, String msg, Throwable throwable) {
        log(LogService.LOG_WARNING, format(marker + ":" + msg), throwable);
    }

    @Override
    public void warn(Marker marker, String msg, Object arg1, Object arg2) {
        log(LogService.LOG_WARNING, format(marker + ":" + msg, arg1, arg2));
    }

    @Override
    public void info(String msg) {
        log(LogService.LOG_ERROR, msg);
    }

    @Override
    public void info(String msg, Object arg1) {

        log(LogService.LOG_INFO, format(msg, arg1));
    }

    @Override
    public void info(String msg, Object[] args) {
        log(LogService.LOG_INFO, format(msg, args));
    }

    @Override
    public void info(String msg, Throwable throwable) {
        log(LogService.LOG_INFO, msg, throwable);
    }

    @Override
    public void info(Marker marker, String msg) {
        log(LogService.LOG_INFO, marker + ":" + msg);
    }

    @Override
    public void info(String msg, Object arg1, Object arg2) {
        log(LogService.LOG_INFO, format(msg, arg1, arg2));
    }

    @Override
    public void info(Marker marker, String msg, Object arg) {
        log(LogService.LOG_INFO, format(marker + ":" + msg, arg));
    }

    @Override
    public void info(Marker marker, String msg, Object[] args) {
        log(LogService.LOG_INFO, format(marker + ":" + msg, args));
    }

    @Override
    public void info(Marker marker, String msg, Throwable throwable) {
        log(LogService.LOG_INFO, format(marker + ":" + msg), throwable);
    }

    @Override
    public void info(Marker marker, String msg, Object arg1, Object arg2) {
        log(LogService.LOG_INFO, format(marker + ":" + msg, arg1, arg2));
    }

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isDebugEnabled(Marker arg0) {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled(Marker arg0) {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled(Marker arg0) {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled(Marker arg0) {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled(Marker arg0) {
		return true;
	}

    @Override
    public void trace(String msg) {
        log(LogService.LOG_ERROR, msg);
    }

    @Override
    public void trace(String msg, Object arg1) {

        log(LogService.LOG_INFO, format(msg, arg1));
    }

    @Override
    public void trace(String msg, Object[] args) {
        log(LogService.LOG_INFO, format(msg, args));
    }

    @Override
    public void trace(String msg, Throwable throwable) {
        log(LogService.LOG_INFO, msg, throwable);
    }

    @Override
    public void trace(Marker marker, String msg) {
        log(LogService.LOG_INFO, marker + ":" + msg);
    }

    @Override
    public void trace(String msg, Object arg1, Object arg2) {
        log(LogService.LOG_INFO, format(msg, arg1, arg2));
    }

    @Override
    public void trace(Marker marker, String msg, Object arg) {
        log(LogService.LOG_INFO, format(marker + ":" + msg, arg));
    }

    @Override
    public void trace(Marker marker, String msg, Object[] args) {
        log(LogService.LOG_INFO, format(marker + ":" + msg, args));
    }

    @Override
    public void trace(Marker marker, String msg, Throwable throwable) {
        log(LogService.LOG_INFO, format(marker + ":" + msg), throwable);
    }

    @Override
    public void trace(Marker marker, String msg, Object arg1, Object arg2) {
        log(LogService.LOG_INFO, format(marker + ":" + msg, arg1, arg2));
    }


    static Pattern LOGMSG_P = Pattern.compile("\\{(\\d+)\\}");
    
	private String format(String msg, Object... args) {
		if ( args==null || args.length==0)
			return msg;
		
		StringBuffer sb = new StringBuffer();
		Matcher m = LOGMSG_P.matcher(msg);
		while(m.find()) {
			int index = Integer.parseInt( m.group(1));
			if ( index >=0 && index < args.length) {
				m.appendReplacement(sb, ""+args[index]);
			} else {
				m.appendReplacement(sb, "{"+index+"}");
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

}
