package org.embeddedt.modernfix.world;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class StrongholdLocationCache extends SavedData {
    private List<ChunkPos> chunkPosList;
    public StrongholdLocationCache() {
        super();
        chunkPosList = new ArrayList<>();
    }

    public List<ChunkPos> getChunkPosList() {
        return new ArrayList<>(chunkPosList);
    }

    public void setChunkPosList(List<ChunkPos> positions) {
        this.chunkPosList = new ArrayList<>(positions);
        this.setDirty();
    }

    public static StrongholdLocationCache load(CompoundTag arg) {
        StrongholdLocationCache cache = new StrongholdLocationCache();
        if(arg.contains("Positions", Tag.TAG_LONG_ARRAY)) {
            long[] positions = arg.getLongArray("Positions");
            for(long position : positions) {
                cache.chunkPosList.add(new ChunkPos(position));
            }
        }
        return cache;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        long[] serialized = new long[chunkPosList.size()];
        for(int i = 0; i < chunkPosList.size(); i++) {
            ChunkPos thePos = chunkPosList.get(i);
            serialized[i] = thePos.toLong();
        }
        compoundTag.putLongArray("Positions", serialized);
        return compoundTag;
    }

    public static String getFileId(Holder<DimensionType> dimensionType) {
        return "mfix_strongholds";
    }
}
