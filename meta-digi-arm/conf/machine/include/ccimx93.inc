#@TYPE: Machine
#@NAME: ConnectCore 93 family of SOMs
#@DESCRIPTION: Machine configuration for Digi's ConnectCore 93 family of SOMs

DIGI_FAMILY = "ccimx93"

require conf/machine/include/imx-digi-base.inc
require conf/machine/include/arm/armv8-2a/tune-cortexa55.inc

# Platform u-boot settings
UBOOT_PREFIX = "imx-boot"
UBOOT_SUFFIX = "bin"

# The bootloader image that gets flashed consists of U-Boot and several fw binaries
EXTRA_IMAGEDEPENDS += "imx-boot"
BOOTLOADER_IMAGE_RECIPE = "imx-boot"
BOOTABLE_FILENAME = "${UBOOT_PREFIX}-${MACHINE}.bin"
SDIMG_BOOTLOADER = "${DEPLOY_DIR_IMAGE}/${BOOTABLE_FILENAME}"

DDR_FIRMWARE_NAME = " \
    lpddr4_dmem_1d_v202201.bin \
    lpddr4_dmem_2d_v202201.bin \
    lpddr4_imem_1d_v202201.bin \
    lpddr4_imem_2d_v202201.bin \
"

MACHINE_FEATURES += "pci wifi bluetooth"
MACHINE_FEATURES:append:use-nxp-bsp = " optee jailhouse"

STORAGE_MEDIA = "mmc"
WLAN_P2P_INTERFACE = "wfd0"

# Linux kernel configuration
KERNEL_DEFCONFIG ?= "arch/arm64/configs/ccimx93_defconfig"

SPL_BINARY = "spl/u-boot-spl.bin"

# Set ATF platform name
ATF_PLATFORM = "imx93"

MACHINE_EXTRA_RDEPENDS += " \
    e2fsprogs-mke2fs \
    e2fsprogs-resize2fs \
    parted \
    xbee-init \
"

# TrustFence
TRUSTFENCE_SIGN_MODE = "AHAB"

IMXBOOT_TARGETS = "flash_singleboot"

IMX_BOOT_SOC_TARGET = "iMX9"
IMX_BOOT_SEEK = "32"

SERIAL_CONSOLES = "115200;ttyLP5"

# Bluetooth tty
BT_TTY ?= "ttyLP0"

# Add additional firmware
MACHINE_FIRMWARE:append = " ethos-u-firmware"

# Per-machine DISTRO_FEATURES customization
MACHINE_DISTRO_FEATURES_REMOVE = "vulkan"

# SWUpdate sw-description configuration
BOOTFS_EXT ?= ".boot.vfat"
ROOTFS_EXT ?= '${@bb.utils.contains("IMAGE_FEATURES", "read-only-rootfs", ".squashfs", ".ext4.gz", d)}'

BOOT_DEV_NAME ?= "/dev/mmcblk0p1"
ROOTFS_DEV_NAME ?= "/dev/mmcblk0p3"
ROOTFS_ENC_DEV = "/dev/mapper/cryptrootfs"
ROOTFS_DEV_NAME_FINAL = "${@oe.utils.ifelse(d.getVar('TRUSTFENCE_ENCRYPT_ROOTFS') == '1', '${ROOTFS_ENC_DEV}', '${ROOTFS_DEV_NAME}')}"
