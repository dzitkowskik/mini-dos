package pl.pw.edu.mini.dos.communication.nodemaster;

import pl.pw.edu.mini.dos.communication.ErrorEnum;

import java.util.HashMap;

public class SelectMetadataResponse {
    public HashMap<String, Integer[]> whichNodeHaveWhichRow;
    public ErrorEnum error;

    public SelectMetadataResponse(HashMap<String, Integer[]> whichNodeHaveWhichRow, ErrorEnum error) {
        this.whichNodeHaveWhichRow = whichNodeHaveWhichRow;
        this.error = error;
    }
}
