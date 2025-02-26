# Copyright (C) 2017-2023, Digi International Inc.

SUMMARY = "Digi's ConnectCore Cloud services"
SECTION = "libs"
LICENSE = "MPL-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MPL-2.0;md5=815ca599c9df247a0c7f619bab123dad"

DEPENDS = "libconfuse libdigiapix openssl recovery-utils swupdate zlib json-c"

SRCBRANCH = "dey-4.0/maint"
SRCREV = "${AUTOREV}"

CC_STASH = "gitsm://git@stash.digi.com/cc/cc_dey.git;protocol=ssh"
CC_GITHUB = "gitsm://github.com/digi-embedded/cc_dey.git;protocol=https"

CC_GIT_URI ?= "${@oe.utils.conditional('DIGI_INTERNAL_GIT', '1' , '${CC_STASH}', '${CC_GITHUB}', d)}"

CCCS_DEVICE_TYPE ?= "${MACHINE}"

SRC_URI = " \
    ${CC_GIT_URI};branch=${SRCBRANCH} \
    file://cccsd-init \
    file://cccsd.service \
    file://cccs-gs-demo-init \
    file://cccs-gs-demo.service \
"

S = "${WORKDIR}/git"

inherit pkgconfig systemd update-rc.d

do_install() {
	oe_runmake DESTDIR=${D} install

	if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
		# Install systemd unit files
		install -d ${D}${systemd_unitdir}/system
		install -m 0644 ${WORKDIR}/cccsd.service ${D}${systemd_unitdir}/system/
		install -m 0644 ${WORKDIR}/cccs-gs-demo.service ${D}${systemd_unitdir}/system/
	fi

	install -d ${D}${sysconfdir}/init.d/
	install -m 755 ${WORKDIR}/cccsd-init ${D}${sysconfdir}/cccsd
	ln -sf /etc/cccsd ${D}${sysconfdir}/init.d/cccsd
	install -m 755 ${WORKDIR}/cccs-gs-demo-init ${D}${sysconfdir}/cccs-gs-demo
	ln -sf /etc/cccs-gs-demo ${D}${sysconfdir}/init.d/cccs-gs-demo

	# Set the device type. Its maximum length is 255 characters
	[ -z "${CCCS_DEVICE_TYPE}" ] && device_type="${MACHINE}" || device_type="${CCCS_DEVICE_TYPE}"
	device_type="$(echo "${device_type}" | cut -c1-255)"
	sed -i "/device_type = .*/c\device_type = \"${device_type}\"" ${D}${sysconfdir}/cccs.conf
}

do_install:append:ccimx6ul() {
	sed -i "/url = \"edp12.devicecloud.com\"/c\url = \"remotemanager.digi.com\"" ${D}${sysconfdir}/cccs.conf
	sed -i "/client_cert_path = \"\/mnt\/data\/drm_cert.pem\"/c\client_cert_path = \"\/etc\/ssl\/certs\/drm_cert.pem\"" ${D}${sysconfdir}/cccs.conf
}

pkg_postinst_ontarget:${PN}() {
	# If dualboot is enabled, change the CCCSD download path on the first boot
	if [ "$(fw_printenv -n dualboot 2>/dev/null)" = "yes" ]; then
		sed -i "/firmware_download_path = \/mnt\/update/c\firmware_download_path = \/home\/root" /etc/cccs.conf
	fi
}

inherit ${@bb.utils.contains("IMAGE_FEATURES", "read-only-rootfs", "remove-pkg-postinst-ontarget", "", d)}

INITSCRIPT_PACKAGES = "${PN}-daemon ${PN}-gs-demo"
INITSCRIPT_NAME:${PN}-daemon = "cccsd"
INITSCRIPT_PARAMS:${PN}-daemon = "defaults 19 81"
INITSCRIPT_NAME:${PN}-gs-demo = "cccs-gs-demo"
INITSCRIPT_PARAMS:${PN}-gs-demo = "defaults 81 19"

SYSTEMD_PACKAGES = "${PN}-daemon ${PN}-gs-demo"
SYSTEMD_SERVICE:${PN}-daemon = "cccsd.service"
SYSTEMD_SERVICE:${PN}-gs-demo = "cccs-gs-demo.service"

PACKAGES =+ " \
    ${PN}-cert \
    ${PN}-daemon \
    ${PN}-gs-demo \
    ${PN}-legacy \
    ${PN}-legacy-dev \
    ${PN}-legacy-staticdev \
"

FILES:${PN}-cert = "${sysconfdir}/ssl/certs/Digi_Int-ca-cert-public.crt"

FILES:${PN}-daemon = " \
    ${bindir}/cccsd \
    ${systemd_unitdir}/system/cccsd.service \
    ${sysconfdir}/cccsd \
    ${sysconfdir}/cccs.conf \
    ${sysconfdir}/init.d/cccsd \
"

FILES:${PN}-gs-demo = " \
    ${bindir}/cccs-gs-demo \
    ${systemd_unitdir}/system/cccs-gs-demo.service \
    ${sysconfdir}/cccs-gs-demo \
"

FILES:${PN}-legacy = " \
    ${bindir}/cloud-connector \
    ${sysconfdir}/cc.conf \
"

FILES:${PN}-legacy-dev = " \
    ${includedir}/cloudconnector \
    ${libdir}/pkgconfig/cloudconnector.pc \
"

FILES:${PN}-legacy-staticdev = " \
    ${libdir}/libcloudconnector.a \
"

CONFFILES:${PN}-daemon += "${sysconfdir}/cccs.conf"

CONFFILES:${PN}-legacy += "${sysconfdir}/cc.conf"

RDEPENDS:${PN}-daemon = "${PN} ${PN}-cert"

RDEPENDS:${PN}-gs-demo = "${PN}-daemon"

RDEPENDS:${PN}-legacy = "${PN} ${PN}-cert"

# 'cccsd-init' and 'cccs-gs-demo-init' scripts use '/etc/init.d/functions'
RDEPENDS:${PN}-daemon += "initscripts-functions"
RDEPENDS:${PN}-gs-demo += "initscripts-functions"

# Disable extra compilation checks from SECURITY_CFLAGS to avoid build errors
lcl_maybe_fortify:pn-cccs = ""
