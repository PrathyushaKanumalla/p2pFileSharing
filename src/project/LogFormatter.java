package project;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
	
	public String format(LogRecord rc) {
		StringBuffer row = new StringBuffer(1000);
		row.append(calcDate(rc.getMillis()));
		row.append(": ");
		row.append(rc.getMessage());
		row.append(".\n");
		return row.toString();
	}

	private String calcDate(long millisecs) {
		SimpleDateFormat date_format = new SimpleDateFormat(Constants.LOGTIMEFORMAT);
		Date resultdate = new Date(millisecs);
		return date_format.format(resultdate);
	}

}
