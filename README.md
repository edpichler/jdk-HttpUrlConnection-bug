# Introduction
This project shows a bug in the `java.net.HttpURLConnection` class.

The `HttpURLConnection` class caps and mess up the body of a big HTTP POST.
If you send a HTTP POST with a small body, it works, but if you send a HTTP POST with, for instance, 10k bytes on its body, it does not work.

## How to run

Just do the command `./gradlew test` and wait for the test results.

If you run it today (19/July/2021) in Mac OS X, **it will not work**. The server will not receive correctly the bytes sent.
If you do the same tests in Linux, it works.

Apparently, the problem happens exactly when BODY content has more than 65839 characters.

It happens in all the java versions I tested (8, 9, 10, 14, 16) under Mac OS X, although, the same code works in Linux.
I think it's a bug on it's core libs, written in C for Mac OS X.