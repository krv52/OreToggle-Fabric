package me.krv.oretoggle;

final class PlayerScanCursor {
    int offsetIndex;
    int lastSeenStateVersion = -1;
    String lastWorldKey = "";
    int lastPlayerX;
    int lastPlayerY;
    int lastPlayerZ;
}
