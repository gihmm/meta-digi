# Copyright (C) 2018 Digi International

require recipes-bsp/u-boot/u-boot.inc

DESCRIPTION = "Bootloader for Digi platforms"
LICENSE = "GPLv2+"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=a2c678cfd4a4d97135585cad908541c6"
SECTION = "bootloaders"

DEPENDS += "bc-native dtc-native u-boot-mkimage-native"
DEPENDS += "${@base_conditional('TRUSTFENCE_SIGN', '1', 'trustfence-sign-tools-native', '', d)}"

PROVIDES += "u-boot"

SRCBRANCH = "v2017.03/maint"
SRCREV = "8336fa4c58dff05372e26eaef5aa3fbb23435ac8"

S = "${WORKDIR}/git"

# Select internal or Github U-Boot repo
UBOOT_GIT_URI ?= "${@base_conditional('DIGI_INTERNAL_GIT', '1' , '${DIGI_GIT}u-boot-denx.git', '${DIGI_GITHUB_GIT}/u-boot.git', d)}"

SRC_URI = " \
    ${UBOOT_GIT_URI};nobranch=1 \
"

SRC_URI_append = " \
    file://boot.txt \
    file://install_linux_fw_sd.txt \
"

LOCALVERSION ?= ""
inherit fsl-u-boot-localversion

EXTRA_OEMAKE_append = " KCFLAGS=-fgnu89-inline"

UBOOT_EXTRA_CONF ?= ""

python __anonymous() {
    if (d.getVar("TRUSTFENCE_DEK_PATH", True) not in ["0", None]) and (d.getVar("TRUSTFENCE_SIGN", True) != "1"):
        bb.fatal("Only signed U-Boot images can be encrypted. Generate signed images (TRUSTFENCE_SIGN = \"1\") or remove encryption (TRUSTFENCE_DEK_PATH = \"0\")")
}

do_compile () {
	if [ "${@bb.utils.filter('DISTRO_FEATURES', 'ld-is-gold', d)}" ]; then
		sed -i 's/$(CROSS_COMPILE)ld$/$(CROSS_COMPILE)ld.bfd/g' ${S}/config.mk
	fi

	unset LDFLAGS
	unset CFLAGS
	unset CPPFLAGS

	if [ ! -e ${B}/.scmversion -a ! -e ${S}/.scmversion ]
	then
		echo ${UBOOT_LOCALVERSION} > ${B}/.scmversion
		echo ${UBOOT_LOCALVERSION} > ${S}/.scmversion
	fi

	if [ -n "${UBOOT_CONFIG}" ]
	then
		unset i j k
		for config in ${UBOOT_MACHINE}; do
			i=$(expr $i + 1);
			for type in ${UBOOT_CONFIG}; do
				j=$(expr $j + 1);
				if [ $j -eq $i ]
				then
					oe_runmake -C ${S} O=${B}/${config} ${config}

					# Reconfigure U-Boot with Digi UBOOT_EXTRA_CONF
					for var in ${UBOOT_EXTRA_CONF}; do
						echo "${var}" >> ${B}/${config}/.config
					done
					oe_runmake -C ${S} O=${B}/${config} oldconfig

					oe_runmake -C ${S} O=${B}/${config} ${UBOOT_MAKE_TARGET}
					for binary in ${UBOOT_BINARIES}; do
						k=$(expr $k + 1);
						if [ $k -eq $i ]; then
							cp ${B}/${config}/${binary} ${B}/${config}/u-boot-${type}.${UBOOT_SUFFIX}
						fi
					done
					unset k

					# Secure boot artifacts
					if [ "${TRUSTFENCE_SIGN}" = "1" ]
					then
						cp ${B}/${config}/u-boot-dtb-signed.imx ${B}/${config}/u-boot-dtb-signed-${type}.${UBOOT_SUFFIX}
						cp ${B}/${config}/u-boot-dtb-usb-signed.imx ${B}/${config}/u-boot-dtb-usb-signed-${type}.${UBOOT_SUFFIX}
						if [ "${TRUSTFENCE_DEK_PATH}" != "0" ]
						then
							cp ${B}/${config}/u-boot-dtb-encrypted.imx ${B}/${config}/u-boot-dtb-encrypted-${type}.${UBOOT_SUFFIX}
						fi
					fi
				fi
			done
			unset  j
		done
		unset  i
	else
		oe_runmake -C ${S} O=${B} ${UBOOT_MACHINE}
		oe_runmake -C ${S} O=${B} ${UBOOT_MAKE_TARGET}
	fi
}

TF_BOOTSCRIPT_SEDFILTER = ""
TF_BOOTSCRIPT_SEDFILTER_ccimx6 = "${@tf_bootscript_sedfilter(d)}"
TF_BOOTSCRIPT_SEDFILTER_ccimx6ul = "${@tf_bootscript_sedfilter(d)}"

def tf_bootscript_sedfilter(d):
    tf_initramfs = d.getVar('TRUSTFENCE_INITRAMFS_IMAGE',True) or ""
    return "s,\(^[[:blank:]]*\)true.*,\\1setenv boot_initrd true\\n\\1setenv initrd_file %s-${MACHINE}.cpio.gz.u-boot.tf,g" % tf_initramfs if tf_initramfs else ""

do_deploy_append() {
	# Remove canonical U-Boot symlinks for ${UBOOT_CONFIG} currently in the form:
	#    u-boot-<platform>.<ext>-<type>
	#    u-boot-<type>
	# and add a more suitable symlink in the form:
	#    u-boot-<platform>-<config>.<ext>
	if [ -n "${UBOOT_CONFIG}" ]
	then
		for config in ${UBOOT_MACHINE}; do
			i=$(expr $i + 1);
			for type in ${UBOOT_CONFIG}; do
				j=$(expr $j + 1);
				if [ $j -eq $i ]
				then
					cd ${DEPLOYDIR}
					rm -r ${UBOOT_BINARY}-${type}
					ln -sf u-boot-${type}-${PV}-${PR}.${UBOOT_SUFFIX} u-boot-${type}.${UBOOT_SUFFIX}
					if [ "${TRUSTFENCE_SIGN}" = "1" ]
					then
						install ${B}/${config}/SRK_efuses.bin SRK_efuses-${PV}-${PR}.bin
						ln -sf SRK_efuses-${PV}-${PR}.bin SRK_efuses.bin

						install ${B}/${config}/u-boot-dtb-signed-${type}.${UBOOT_SUFFIX} u-boot-dtb-signed-${type}-${PV}-${PR}.${UBOOT_SUFFIX}
						ln -sf u-boot-dtb-signed-${type}-${PV}-${PR}.${UBOOT_SUFFIX} u-boot-dtb-signed-${type}.${UBOOT_SUFFIX}

						install ${B}/${config}/u-boot-dtb-usb-signed-${type}.${UBOOT_SUFFIX} u-boot-dtb-usb-signed-${type}-${PV}-${PR}.${UBOOT_SUFFIX}
						ln -sf u-boot-dtb-usb-signed-${type}-${PV}-${PR}.${UBOOT_SUFFIX} u-boot-dtb-usb-signed-${type}.${UBOOT_SUFFIX}

						if [ "${TRUSTFENCE_DEK_PATH}" != "0" ]
						then
							install ${B}/${config}/u-boot-dtb-encrypted-${type}.${UBOOT_SUFFIX} u-boot-dtb-encrypted-${type}-${PV}-${PR}.${UBOOT_SUFFIX}
							ln -sf u-boot-dtb-encrypted-${type}-${PV}-${PR}.${UBOOT_SUFFIX} u-boot-dtb-encrypted-${type}.${UBOOT_SUFFIX}
						fi
					fi
				fi
			done
			unset  j
		done
		unset  i
	fi

	# DEY firmware install script
	sed -i -e 's,##GRAPHICAL_BACKEND##,${GRAPHICAL_BACKEND},g' ${WORKDIR}/install_linux_fw_sd.txt
	mkimage -T script -n "DEY firmware install script" -C none -d ${WORKDIR}/install_linux_fw_sd.txt ${DEPLOYDIR}/install_linux_fw_sd.scr

	# Boot script for DEY images (reconfigure on-the-fly if TRUSTFENCE is enabled)
	TMP_BOOTSCR="$(mktemp ${WORKDIR}/bootscr.XXXXXX)"
	sed -e "${TF_BOOTSCRIPT_SEDFILTER}" ${WORKDIR}/boot.txt > ${TMP_BOOTSCR}
	mkimage -T script -n bootscript -C none -d ${TMP_BOOTSCR} ${DEPLOYDIR}/boot.scr

	# Sign the scripts
	if [ "${TRUSTFENCE_SIGN}" = "1" ]; then
		export CONFIG_SIGN_KEYS_PATH="${TRUSTFENCE_SIGN_KEYS_PATH}"
		[ -n "${TRUSTFENCE_KEY_INDEX}" ] && export CONFIG_KEY_INDEX="${TRUSTFENCE_KEY_INDEX}"
		[ -n "${TRUSTFENCE_DEK_PATH}" ] && [ "${TRUSTFENCE_DEK_PATH}" != "0" ] && export CONFIG_DEK_PATH="${TRUSTFENCE_DEK_PATH}"

		# Sign boot script
		TMP_SIGNED_BOOTSCR="$(mktemp ${WORKDIR}/bootscr-signed.XXXXXX)"
		trustfence-sign-kernel.sh -p "${DIGI_FAMILY}" -b "${DEPLOYDIR}/boot.scr" "${TMP_SIGNED_BOOTSCR}"
		mv "${TMP_SIGNED_BOOTSCR}" "${DEPLOYDIR}/boot.scr"
	fi
	rm -f ${TMP_BOOTSCR}
}

BOOT_TOOLS = "imx-boot-tools"

do_deploy_append_ccimx8x() {
	# Move all U-Boot artifacts to the imx-boot-tools folder
	# U-Boot images are not bootable on the i.MX8X
	install -d ${DEPLOYDIR}/${BOOT_TOOLS}
	mv ${DEPLOYDIR}/u-boot* ${DEPLOYDIR}/${BOOT_TOOLS}/
}

COMPATIBLE_MACHINE = "(ccimx6$|ccimx6ul|ccimx8x)"
