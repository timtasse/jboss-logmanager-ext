package org.jboss.logmanager.ext.handlers;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.handlers.PeriodicRotatingFileHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * FileHandler for JBoss Logmanager which rotates the logfile periodic (PeriodicRotatingFileHandler)
 * additionally zips the old logfile and can handle the maxBackups from the SizeRotatingFileHandler
 * Created by timtasse on 23.12.15.
 */
public class ZippedPeriodicRotatingFileHandler extends PeriodicRotatingFileHandler {

    /**
     * Supported Zip Formats for rotating
     */
    public enum ZipFormat {
        /**
         * Gzip format
         */
        GZIP,
        /**
         * Zip Format
         */
        ZIP
    }

    private ZipFormat zipFormat;
    private int maxBackups = 0;

    /**
     * Default Constructor, use GZIP and Append=true as default
     * because wrong order of calling setter methods, append=true is never working if not set here
     */
    public ZippedPeriodicRotatingFileHandler() {
        super();
        this.zipFormat = ZipFormat.GZIP;
        this.setAppend(true);
    }

    /**
     * Wraps the original preWrite Method and checks if the nextSuffix is changing,
     * this is an indicator of the logfile rotation
     * if the logfile is rotated, zip the old logfile and check the maximum number of backups
     * @param record LogRecord
     */
    protected void preWrite(ExtLogRecord record) {
        final String oldSuffix = this.getNextSuffix();
        super.preWrite(record);
        if (this.getNextSuffix() != null && !this.getNextSuffix().equals(oldSuffix)) {
            final JobRunner jobRunner = new JobRunner(oldSuffix);
            new Thread(jobRunner).start();
        }
    }


    public ZipFormat getZipFormat() {
        return zipFormat;
    }

    public void setZipFormat(final ZipFormat zipFormat) {
        System.out.println("setZipFormat: " + zipFormat);
        this.zipFormat = zipFormat;
    }

    public int getMaxBackups() {
        return maxBackups;
    }

    public void setMaxBackups(final int maxBackups) {
        System.out.println("setMaxBackups: " + maxBackups);
        this.maxBackups = maxBackups;
    }

    @Override
    public void setAppend(final boolean append) {
        System.out.println("setAppend: " + append);
        super.setAppend(append);
    }

    @Override
    public void setAutoFlush(final boolean autoFlush) throws SecurityException {
        System.out.println("setAutoFlush: " + autoFlush);
        super.setAutoFlush(autoFlush);
    }

    @Override
    public void setFileName(final String fileName) throws FileNotFoundException {
        System.out.println("setFileName: " + fileName);
        super.setFileName(fileName);
    }

    /**
     * Zipping oldLog and deletes files
     * non blocking for the Logging Thread
     */
    class JobRunner implements Runnable {

        private String oldSuffix;

        public JobRunner(final String oldSuffix) {
            this.oldSuffix = oldSuffix;
        }

        @Override
        public void run() {
            this.zippingOldLog(this.oldSuffix);
            this.checkMaxBackups();
        }

        /**
         * Check if the maximum number of backups is reached and delete the files that are too much
         */
        private void checkMaxBackups() {
            if (getMaxBackups() < 1) {
                return;
            }
            final File file = getFile();
            final File[] listFiles = file.getParentFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name.startsWith(file.getName()) && name.endsWith(".gz");
                }
            });
            if (listFiles.length > getMaxBackups()) {
                final List<File> fileList = Arrays.asList(listFiles);
                Collections.sort(fileList);
                Collections.reverse(fileList);
                final List<File> deleteList = fileList.subList(getMaxBackups(), fileList.size());
                this.deleteFiles(deleteList);
            }
        }

        /**
         * Deletes the List of Files
         * @param files List of Files
         */
        private void deleteFiles(final List<File> files) {
            for (File file : files) {
                System.out.println("Delete File " + file.getName());
                file.delete();
            }
        }

        /**
         * Zips the old logfile with the requested zip format
         * @param suffix the old suffix from the rotated logfile
         */
        private void zippingOldLog(final String suffix) {
            System.out.println("zippingOldLog in format " + zipFormat);
            final File file = new File(getFile() + suffix);
            if (!file.exists() || !file.isFile()) {
                reportError("File to zip not existing " + file.getAbsolutePath(),
                        new FileNotFoundException(file.getAbsolutePath()), ErrorManager.GENERIC_FAILURE);
                return;
            }
            switch (zipFormat) {
                case GZIP:
                    System.out.println("zippingOldLogWithGzip");
                    this.zippingOldLogWithGzip(file);
                    break;
                case ZIP:
                    System.out.println("zippingOldLogWithZip");
                    this.zippingOldLogWithZip(file);
                    break;
            }
        }

        /**
         * Zipping the File with Gzip
         * @param file file to gzip
         */
        private void zippingOldLogWithGzip(final File file) {
            if (!file.exists()) {
                reportError("File to zip not existing " + file.getAbsolutePath(),
                        new FileNotFoundException(file.getAbsolutePath()), ErrorManager.GENERIC_FAILURE);
                return;
            }
            byte[] buffer = new byte[1024];
            try {
                GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(file.getAbsolutePath() + ".gz"));
                FileInputStream in = new FileInputStream(file);
                int len;
                while ((len = in.read(buffer)) > 0) {
                    gzos.write(buffer, 0, len);
                }
                in.close();
                gzos.finish();
                gzos.close();
                file.delete();
            } catch(IOException ex) {
                reportError("Could not write gzipped File", ex, ErrorManager.WRITE_FAILURE);
            }
        }

        /**
         * Zipping the File with Zip
         * @param file file to zip
         */
        private void zippingOldLogWithZip(final File file) {
            if (!file.exists()) {
                reportError("File to zip not existing " + file.getAbsolutePath(),
                        new FileNotFoundException(file.getAbsolutePath()), ErrorManager.GENERIC_FAILURE);
                return;
            }
            byte[] buffer = new byte[1024];
            try {
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file.getAbsolutePath() + ".zip"));
                final ZipEntry zipEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);
                FileInputStream in = new FileInputStream(file);
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                in.close();
                zos.closeEntry();
                zos.close();
                file.delete();
            } catch(IOException ex) {
                reportError("Could not write zipped File", ex, ErrorManager.WRITE_FAILURE);
            }
        }
    }
}
