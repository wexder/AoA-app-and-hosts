{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    devenv.url = "github:cachix/devenv";
    flutter-nix = {
      url = "github:maximoffua/flutter.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs/516bd59";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    anixpkgs.url = "github:goromal/anixpkgs";
  };

  outputs = inputs@{ nixpkgs, flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      imports = [
        inputs.devenv.flakeModule
      ];
      systems = [
        "x86_64-linux"
        "aarch64-darwin"
      ];

      perSystem = { config, self', inputs', pkgs, system, ... }:
        let
          androidSdk = pkgs.androidenv.androidPkgs;
          androidComposition = pkgs.androidenv.composeAndroidPackages {
            cmdLineToolsVersion = "8.0";
            toolsVersion = "26.1.1";
            platformToolsVersion = "34.0.5";
            buildToolsVersions = [ "34.0.0" ];
            includeEmulator = false;
            platformVersions = [ "30" "34" ];
            includeSources = false;
            includeSystemImages = false;
            systemImageTypes = [ "google_apis_playstore" ];
            abiVersions = [ "armeabi-v7a" "arm64-v8a" ];
            cmakeVersions = [ "3.10.2" ];
            includeNDK = false;
            ndkVersions = [ "22.0.7026061" ];
            useGoogleAPIs = false;
            useGoogleTVAddOns = false;
            includeExtras = [ ];
          };
        in
        {
          _module.args.pkgs = import nixpkgs {
            inherit system;
            config = {
              allowUnfree = true;
              android_sdk.accept_license = true;
            };
          };

          devenv.shells.default = {
            env.ANDROID_SDK_ROOT = "${androidComposition.androidsdk}/share/android-sdk";
            env.ANDROID_HOME = "${androidComposition.androidsdk}/share/android-sdk";
            env.CHROME_EXECUTABLE = "chromium";
            env.FLUTTER_SDK = "${pkgs.flutter}";
            env.JAVA_21 = "${pkgs.jdk21}";
            env.JAVA_HOME = "${pkgs.jdk17}";
            env.GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidComposition.androidsdk}/libexec/android-sdk/build-tools/34.0.0/aapt2 -Dorg.gradle.project.android.aaptFromMavenOverride=${androidComposition.androidsdk}/libexec/android-sdk/build-tools/34.0.0/aapt";
            # env.LIBCLANG_PATH = "${pkgs.llvmPackages.libclang.lib}/lib";
            env.LD_LIBRARY_PATH = "$LD_LIBRARY_PATH:${pkgs.libusb1.out}/lib:${pkgs.libuvc.out}/lib:${pkgs.stdenv.cc.cc.lib}/lib:${pkgs.libGL}/lib:${pkgs.glib.out}/lib:${pkgs.libz.out}/lib:${pkgs.libjpeg8.out}/lib";
            env.LIBRARY_PATH = "$LD_LIBRARY_PATH:${pkgs.libusb1.out}/lib:${pkgs.libuvc.out}/lib:${pkgs.stdenv.cc.cc.lib}/lib:${pkgs.libGL}/lib:${pkgs.glib.out}/lib:${pkgs.libz.out}/lib:${pkgs.libjpeg8.out}/lib";

            packages = [
              pkgs.cmake
              pkgs.gradle_7
              (pkgs.android-studio.withSdk (androidComposition.androidsdk))
              pkgs.android-tools
              androidComposition.androidsdk
            ];


            enterShell = ''
            '';
          };
        };
    };
}

