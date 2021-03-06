# Termux API

[![Build status](https://api.cirrus-ci.com/github/termux/termux-api.svg?branch=master)](https://cirrus-ci.com/termux/termux-api)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

This is an app exposing Android API to command line usage and scripts or programs.

When developing or packaging, note that this app needs to be signed with the same
key as the main Termux app for permissions to work (only the main Termux app are
allowed to call the API methods in this app).

## Installation

Termux:API application can be obtained from:

- [Google Play](https://play.google.com/store/apps/details?id=com.termux.api)
- [F-Droid](https://f-droid.org/en/packages/com.termux.api/)
- [Kali Nethunter Store](https://store.nethunter.com/en/packages/com.termux.api/)

Additionally we offer development builds for those who want to try out latest
features ready to be included in future versions. Such build can be obtained
directly from [Cirrus CI artifacts](https://api.cirrus-ci.com/v1/artifact/github/termux/termux-api/debug-build/output/app/build/outputs/apk/debug/app-debug.apk).

Signature keys of all offered builds are different. Before you switch the
installation source, you will have to uninstall the Termux application and
all currently installed plugins.

## License

Released under the [GPLv3 license](http://www.gnu.org/licenses/gpl-3.0.en.html).

## How API calls are made through the termux-api helper binary

The [termux-api](https://github.com/termux/termux-api-package/blob/master/termux-api.c)
client binary in the `termux-api` package generates two linux anonymous namespace
sockets, and passes their address to the [TermuxApiReceiver broadcast receiver](https://github.com/termux/termux-api/blob/master/app/src/main/java/com/termux/api/TermuxApiReceiver.java)
as in:

```
/system/bin/am broadcast ${BROADCAST_RECEIVER} --es socket_input ${INPUT_SOCKET} --es socket_output ${OUTPUT_SOCKET}
```

The two sockets are used to forward stdin from `termux-api` to the relevant API
class and output from the API class to the stdout of `termux-api`.

## Client scripts

Client scripts which processes command line arguments before calling the
`termux-api` helper binary are available in the [termux-api package](https://github.com/termux/termux-api-package).

## Ideas

- Wifi network search and connect.
- Add extra permissions to the app to (un)install apps, stop processes etc.
