# Al'Trader

---

[![Build Status](https://travis-ci.org/romatroskin/altrader.svg?branch=master)](https://travis-ci.org/romatroskin/altrader)

## Description

A techincal analysis bot for trading poloniex using TA and some strategies like, [RSI-2][RSI_URL], [Ichimoku Signals][ICHIMOKU_URL]

## Building instructions

The project is using [Gradle][GRADLE_URL] Build Tool.
Building is easy as one-two-three :

* **Mac OS X**

  * Install [Homebrew][BREW_URL]
  ```bash
  /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
  ```
  * Install all the dependencies
  ```bash
  brew install gradle
  ```
* **Linux**
    * Install [SDKMAN][SDKMAN_URL]
    ```bash
    curl -s "https://get.sdkman.io" | bash
    ```
    * Install all the dependencies
    ```bash
    sdk install gradle 4.4.1
    ```
* **Windows**
    * Install [Scoop][SCOOP_URL] or [Chocolatey][CHOCO_URL]. If you are using or want to start using Scoope, make sure [Powershell 3][POWERSHELL_URL] is installed, then run
    ```powershell
    iex (new-object net.webclient).downloadstring('https://get.scoop.sh')
    ```
    After installation finished successfully, run
    ```batch
    scoop install gradle
    ```
    If you prefer Chocolatey, then you can use `cmd.exe`
    ```batch
    @"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -InputFormat None -ExecutionPolicy Bypass -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))" && SET "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"
    ```
    After installation finished successfully, run
    ```batch
    choco install gradle
    ```
* **Clone repository and build the project**
    ```bash
    git clone https://github.com/romatroskin/altrader.git
    cd altrader
    gradle build
    ```

## Authors

* **Roman Matroskin** - <romatroskin@gmail.com>

See also the list of [contributors](https://github.com/romatroskin/altrader/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the LICENSE.md file for details

### That's it

---

> Roman Matroskin <romatroskin@gmail.com>

[GRADLE_URL]: https://gradle.org/
[BREW_URL]: https://brew.sh/
[SDKMAN_URL]: http://sdkman.io/
[SCOOP_URL]: http://scoop.sh/
[CHOCO_URL]: https://chocolatey.org/
[POWERSHELL_URL]: http://www.microsoft.com/en-us/download/details.aspx?id=34595
[RSI_URL]: http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
[ICHIMOKU_URL]: http://www.ichimokutrader.com/signals.html