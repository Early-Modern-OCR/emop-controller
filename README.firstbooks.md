This README is specific to FirstBooks project.

#### Table of Contents

1. [Install](#install)
2. [Build](#build)
3. [Setup](#setup)
4. [Usage](#usage)
5. [Support](#support)
6. [Development](#development)

## Install

Clone this repository and merge in the submodules

    git clone https://github.com/Early-Modern-OCR/emop-controller.git
    cd emop-controller

Copy the modulefiles.example directory to modulefiles and set the firstbooks module to be loaded as the emop module.

    cp -r modulefiles.example modulefiles
    cp modulefiles/firstbooks.lua modulefiles/emop.lua

## Build

If modules are not already built for things like Ocular, they must be built.  See the module Lua files for steps on how these programs can be built.

## Setup

For multiple users to run this controller on the same file structure the umask must be set to at least 002.

Add the following to your login scripts such as ~/.bashrc

    umask 002

Rename the following configuration files and change their values as needed:

* config.ini.example-firstbooks to config.ini

The file `config.ini` contains all the configuration options used by the emop-controller.

## Usage

See [README](README.md)

## Development

### System tests

Verify transfers can be initiated and possibly update config to appropriate values

    ./emopcmd.py -c tests/system/config-firstbooks-test.ini transfer status

To run the test using stakeholder or stakeholder-4g partition:

    sbatch -p stakeholder,stakeholder-4g tests/system/firstbooks-test.slrm

Check the output of `logs/test/emop-controller-test-JOBID.out` where JOBID is the value output when sbatch was executed.
