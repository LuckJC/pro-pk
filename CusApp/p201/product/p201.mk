
###############################################################################
### Add 3rd Apps and Service
PRODUCT_PACKAGES += \
		Flashlight \
		HelloTV1 \
		LemonCare \
		Lemonmobiles \
		LemonSalesTracker_production \
		Mojo \
		com.dataviz.docstogo \
		operamini \
		PaniniKeypadAssamese_IME \
		PaniniKeypadBengali_IME \
		PaniniKeypadBhojpuri_IME \
		PaniniKeypadGujarati_IME \
		PaniniKeypadHindi_IME \
		PaniniKeypadKannada_IME \
		PaniniKeypadMalayalam_IME \
		PaniniKeypadMarathi_IME \
		PaniniKeypadNepali_IME \
		PaniniKeypadOriya_IME \
		PaniniKeypadPunjabi_IME \
		PaniniKeypadTamil_IME \
		PaniniKeypadTelugu_IME \
		PaniniKeypadUrdu_IME
        
###############################################################################

#lib
PRODUCT_COPY_FILES += \
	packages/CusApp/p201/lib/Flashlight/libnmsp_speex.so:system/lib/libnmsp_speex.so \
	packages/CusApp/p201/lib/operamini/libom.so:system/lib/libom.so