package com.engine.dawnstar.main.data;

import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.engine.dawnstar.client.DawnStar;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class ChunkStorage {

    public final AtomicReferenceArray<ChunkColumn> storage;
    public final int viewRange;
    public int centerX;
    public int centerZ;

    public ChunkStorage(int renderDistance){
        this.viewRange = (renderDistance * 2) + 1;
        this.storage = new AtomicReferenceArray<>(viewRange * viewRange);
        this.centerX = 0;
        this.centerZ = 0;
    }

    public void addOrReplaceColumn(ChunkColumn column){
        int index = getIndex(column.x, column.z);
        storage.set(index,column);
    }

    public Chunk getChunk(int x,int y, int z){
        int index = getIndex(x, z);
        ChunkColumn column =  storage.get(index);
        if (column != null && (column.x != x || column.z != z)) {
//            storage.set(index, null);
            return null;
        }
        return column != null ? column.getChunk(y) : null;
    }

    public ChunkColumn getColumn(int x, int z){
        int index = getIndex(x, z);
        ChunkColumn chunkColumn = storage.get(index);
        if (chunkColumn != null && (chunkColumn.x != x || chunkColumn.z != z)) {
//            storage.set(index, null);
            return null;
        }
        return chunkColumn;
    }

    private int getIndex(int x,int z){
        return Math.floorMod(z,viewRange) * viewRange + Math.floorMod(x,viewRange);
    }

    public void updateCenter(int centerX,int centerZ){
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public boolean isInViewRange(GridPoint3 playerPos, Vector3 direction, ChunkColumn column){
        float dx = column.x - playerPos.x;
        float dz = column.z - playerPos.z;
        float distanceX = Math.abs(dx);
        float distanceZ = Math.abs(dz);
        if (distanceX <= 1 && distanceZ <= 1) return true;
        double magnitude = Math.sqrt((dx * dx) + (dz * dz));
        double nx = dx / magnitude;
        double nz = dz / magnitude;
        double dotProduct = (nx * direction.x)  + (nz * direction.z);
        return dotProduct >= 0;
    }

    public boolean isInRange(int xc,int zc){
        float dx = Math.abs(xc - centerX);
        float dz = Math.abs(zc - centerZ);
        int halfView = viewRange / 2;
        return dx <= halfView && dz <= halfView;
    }

    public boolean isInLightRange(int xc,int zc,int range){
        float dx = Math.abs(xc - centerX);
        float dz = Math.abs(zc - centerZ);
        return dx < range && dz < range;
    }

    public void unloadOutOfRangeChunks() {
        int halfView = viewRange / 2;
        for (int worldX = centerX - halfView; worldX <= centerX + halfView; worldX++) {
            for (int worldZ = centerZ - halfView; worldZ <= centerZ + halfView; worldZ++) {
                int idx = getIndex(worldX, worldZ);
                ChunkColumn col = storage.get(idx);
                if (col != null && !isInRange(col.x, col.z)) {
                    storage.set(idx, null);
                }
            }
        }
    }

    public void unloadChunksAndBuffer(int renderDistance) {
        int halfView = viewRange / 2;
        for (int x = -halfView; x <= halfView; x++) {
            for (int z = -halfView; z <= halfView; z++) {
                int worldX = x + centerX;
                int worldZ = z + centerZ;
                int idx = getIndex(worldX, worldZ);
                ChunkColumn col = storage.get(idx);
                if (col == null) continue;
                if (x >= -renderDistance && x <= renderDistance){
                    if (z >= -renderDistance && z <= renderDistance){
                        col.flagNew = true;
                    }
                }
                if (!isInRange(col.x, col.z)) {
                    storage.set(idx, null);
                }
            }
        }
    }

    public boolean isEmpty(int xc, int zc) {
        ChunkColumn column = getColumn(xc, zc);
        return (column == null);
    }
}
