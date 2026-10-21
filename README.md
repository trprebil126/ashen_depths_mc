# Ashen Depths

Ashen Depths is a Fabric mod for Minecraft 26.3. It adds Emberite ore and an ash-covered mountain dimension that can be reached through a custom portal.

## Emberite

Emberite ore generates in deepslate in Overworld biomes, following vanilla diamond ore's depth and placement patterns. It does not generate in the Ashen Depths dimension. Mining it drops Raw Emberite, which can be smelted or blasted into Emberite Ingots. Nine ingots craft a Block of Emberite, and the block can be crafted back into nine ingots.

## Portal

Build a rectangular frame from Blocks of Emberite, with an empty opening at least 2 blocks wide and 3 blocks tall. The largest supported opening is 21 by 21 blocks. Use a Lava Bucket on the bottom row of the frame to activate it. In survival mode, the bucket is returned empty.

The portal creates a linked counterpart near the corresponding Nether-scaled coordinates. If there is no nearby portal, it creates one on safe ground.

## Build

Install JDK 25 and run this from the project folder:

```powershell
.\gradlew.bat build
```

The mod jar is written to `build\libs\ashen-depths-1.0.0.jar`. Install it with Fabric Loader 0.19.5 or newer and Fabric API for Minecraft 26.3.

## Ashen Depths dimension

An open-sky world of Ash Block surfaces over blackstone and basalt, with hills and mountains, lava lowlands and drifting white ash particles.
