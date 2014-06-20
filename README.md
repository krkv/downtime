downtime
========

A simple Java program that tracks the website downtime.

Program works in command line mode.

On startup user has to provide:

1. Website address to monitor
2. Interval between checks (in seconds)
3. Number of failed checks to send an email notification

And configure email notifications with:

1. SMTP host
2. SMTP username
3. SMTP password
4. Notification receiver email address

Program monitors website status with given interval.

Downtime information is saved to log file.

According to user configuration, email notification is sent:

1. When site goes down (contains HTTP error status)
2. When site goes back up
