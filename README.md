
<!-- Variables -->

[resourceId]: 106821

[ratingImage]: https://img.shields.io/badge/dynamic/json.svg?color=brightgreen&label=rating&query=%24.rating.average&suffix=%20%2F%205&url=https%3A%2F%2Fapi.spiget.org%2Fv2%2Fresources%2F106821
[buildImage]: https://github.com/M0diis/M0-SafeCreative/actions/workflows/gradle.yml/badge.svg
[downloadsImage]: https://img.shields.io/badge/dynamic/json.svg?color=brightgreen&label=downloads%20%28spigotmc.org%29&query=%24.downloads&url=https%3A%2F%2Fapi.spiget.org%2Fv2%2Fresources%2F106821
[updatedImage]: https://badges.pufler.dev/updated/M0diis/M0-SafeCreative

<!-- End of variables block -->

![build][buildImage] 
![downloads][downloadsImage] ![rating][ratingImage]

## SafeCreative
A server owners dream to make creative mode as safe as possible.

### Development
Building is quite simple.

To build SafeCreative, you need JDK 16 or higher and Gradle installed on your system.

Clone the repository or download the source code from releases.  
Run `gradlew shadowjar` to build the jar.  
The jar will be found created in `/build/libs/` folder. 

**Building**
```
git clone https://github.com/M0diis/M0-SafeCreative.git
cd M0-SafeCreative
gradlew shadowjar
```

### Dev-builds

All the development builds can be found on actions page.
Open the workflow and get the artifact from there.

https://github.com/M0diis/M0-SafeCreative/actions

#### Links

- [Spigot Page](https://www.spigotmc.org/resources/106821/)
- [Issues](https://github.com/M0diis/M0-SafeCreative/issues)
  - [Bug report](https://github.com/M0diis/M0-SafeCreative/issues)
  - [Feature request](https://github.com/M0diis/M0-SafeCreative/issues)
- [Pull requests](https://github.com/M0diis/M0-SafeCreative/pulls)

##### APIs
- [bStats](https://github.com/Bastian/bStats)

