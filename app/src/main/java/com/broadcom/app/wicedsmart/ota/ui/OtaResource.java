package com.broadcom.app.wicedsmart.ota.ui;

import java.io.InputStream;

public interface OtaResource {
    public String getName();

    public int getAppId();

    public int getMajor();

    public int getMinor();

    public long getLength();

    public InputStream getStream();

    public void closeStream();

    public boolean isMandatory();
}