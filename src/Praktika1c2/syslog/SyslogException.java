package Praktika1c2.syslog;

import java.lang.Exception;

public class SyslogException extends Exception
{
    SyslogException()
    {
        super();
    }

    SyslogException( String msg )
    {
        super( msg );
    }
}