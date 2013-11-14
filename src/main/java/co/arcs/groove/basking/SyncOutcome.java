package co.arcs.groove.basking;

public class SyncOutcome {

	public final int deleted;
	public final int downloaded;
	public final int failedToDownload;

	public SyncOutcome(int deleted, int downloaded, int failedToDownload) {
		this.deleted = deleted;
		this.downloaded = downloaded;
		this.failedToDownload = failedToDownload;
	}
}
