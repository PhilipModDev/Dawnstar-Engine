package com.engine.dawnstar.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.engine.dawnstar.client.entities.Entity;
import com.engine.dawnstar.client.entities.LocalPlayer;
import com.engine.dawnstar.client.graphics.Camera;
import com.engine.dawnstar.client.graphics.CameraUnit;
import com.engine.dawnstar.client.graphics.GameGraphicsUnit;
import com.engine.dawnstar.main.data.*;
import com.engine.dawnstar.main.gen.ClientLevel;
import com.engine.dawnstar.main.mesh.*;
import com.engine.dawnstar.utils.ShaderUtils;
import com.engine.dawnstar.utils.math.FrustumUtil;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.badlogic.gdx.Gdx.gl32;
import static com.engine.dawnstar.client.DawnStar.RENDER_DISTANCE;

public final class WorldRenderer implements Disposable {

    public static final int MAX_QUEUE = 500;
    private final Array<Entity> entities = new Array<>();
    private final Array<ChunkMesh> liquidBuffer;
    private final Array<ChunkMesh> terrainBuffer;
    private final Array<ChunkMesh> transparentBuffer;
    public  final Deque<ChunkMeshHolder> meshQueue;
    private final ClientLevel clientLevel;
    private final DawnStar client;
    private final GridPoint3 previousPos;
    private final GridPoint3 presentPos;
    private final CameraUnit cameraUnit;
    private final ShaderUtils shaderUtils;
    private final Texture texture;
    private int ticks = 0;
    private final SkyRenderer skyRenderer;

    public WorldRenderer(DawnStar client, GameGraphicsUnit gameGraphicsUnit){
        this.cameraUnit = gameGraphicsUnit.getCameraUnit();
        LocalPlayer localPlayer = client.getLocalPlayer();
        texture = new Texture(Gdx.files.internal("textures/player_face.png"));
        this.client = client;
        clientLevel = new ClientLevel(this);
        liquidBuffer = new Array<>();
        terrainBuffer = new Array<>();
        transparentBuffer = new Array<>();
        meshQueue = new ArrayDeque<>(MAX_QUEUE);
        presentPos = localPlayer.getPlayerChunkPos();
        previousPos = localPlayer.getPreviousChunkPos();
        this.shaderUtils = gameGraphicsUnit.getShaderUtils();
        //Creates the sky.
        skyRenderer = new SkyRenderer(shaderUtils,client);
    }

    //Binds for voxel rendering.
    public void render() {
        float brightness = skyRenderer.getSun().brightness;
        client.getGameData().getTextureAtlas().bind();
        skyRenderer.render();
        gl32.glEnable(GL32.GL_CULL_FACE);
        gl32.glEnable(GL32.GL_DEPTH_TEST);
        gl32.glDepthFunc(GL32.GL_LEQUAL);
        Vector3 fogColor = skyRenderer.getFogColor();
        GridPoint3 playerChunkPos = client.getPlayerChunkPos();
        Camera camera = cameraUnit.getMainCamera();
        shaderUtils.bindVoxelShaders(fogColor);
        clientLevel.chunkBuilder.bindModelBuffer();
        renderTerrain(brightness,playerChunkPos,camera);


        gl32.glEnable(GL32.GL_BLEND);
        gl32.glBlendFunc(GL32.GL_SRC_ALPHA,GL32.GL_ONE_MINUS_SRC_ALPHA);
        gl32.glDisable(GL32.GL_CULL_FACE);
        gl32.glDepthMask(true);

        renderTransparent(playerChunkPos);
        renderLiquid(playerChunkPos);

        client.getRayCast().renderBox();
        gl32.glDisable(GL32.GL_BLEND);
        //Renders all entities.
        if (entities.notEmpty()){
            texture.bind();
            shaderUtils.bindMesh();
            for (int i = entities.size - 1; i >= 0; i--){
                Entity entity = entities.get(i);
                entity.render(camera,shaderUtils.getVoxelMesh());
            }
        }
    }

    private void renderLiquid(GridPoint3 playerChunkPos) {
        if (!liquidBuffer.isEmpty()){
            ShaderProgram voxelShader = shaderUtils.getVoxelShader();
            for (int i = liquidBuffer.size - 1; i >= 0; i--) {
                ChunkMesh liquid = liquidBuffer.get(i);
                //Dispose the mesh if out of render distance.
                if (isOutRenderDistance(liquid,playerChunkPos)) {
                    liquid.chunk.setFlagVisible(false);
                    liquid.dispose();
                    liquidBuffer.removeIndex(i);
                    continue;
                }
                if (canCullTransparent(liquid.chunk)) continue;
                Chunk chunk = liquid.chunk;
                if (!chunk.flagVisible || chunk.isCulled){
                    continue;
                }
                liquid.render(voxelShader,true);
            }
        }
    }

    private void renderTransparent(GridPoint3 playerChunkPos) {
        if (!transparentBuffer.isEmpty()){
            ShaderProgram voxelShader = shaderUtils.getVoxelShader();
            for (int i = transparentBuffer.size - 1; i >= 0; i--) {
                ChunkMesh transparent = transparentBuffer.get(i);
                //Dispose the mesh if out of render distance.
                if (isOutRenderDistance(transparent,playerChunkPos)) {
                    transparent.chunk.setFlagVisible(false);
                    transparent.dispose();
                    transparentBuffer.removeIndex(i);
                    continue;
                }
                if (canCullTransparent(transparent.chunk)) continue;
                Chunk chunk = transparent.chunk;
                if (!chunk.flagVisible || chunk.isCulled){
                    continue;
                }
                transparent.render(voxelShader,false);
            }
        }
    }

    private void renderTerrain(float sunLightIntensity,GridPoint3 playerChunkPos, Camera camera) {
        if (!terrainBuffer.isEmpty()){
            ShaderProgram terrainShader = shaderUtils.getVoxelShader();
            terrainShader.setUniformf("sunLightIntensity", sunLightIntensity);
            for (int i = terrainBuffer.size - 1; i >= 0; i--) {
                ChunkMesh terrain = terrainBuffer.get(i);
                if (isOutRenderDistance(terrain,playerChunkPos)) {
                    terrain.chunk.setFlagVisible(false);
                    terrain.dispose();
                    terrainBuffer.removeIndex(i);
                    continue;
                }
                Chunk chunk = terrain.chunk;
                if (!chunk.flagVisible) continue;
                if (!FrustumUtil.frustumBounds(camera.frustum.planes, chunk)){
                    chunk.isCulled = true;
                    continue;
                } else chunk.isCulled = false;

                terrain.render(terrainShader,false);
            }
        }
    }

    private long endTime = 0;
    //Updates
    public void update() {
        skyRenderer.update();
        if (ticks >= 20) {
            clientLevel.clientChunkCache.checkChunks(presentPos,previousPos, RENDER_DISTANCE);
            ticks = 0;
        }
        if (endTime - System.currentTimeMillis() <= 0){
            int max = 50;
            endTime = System.currentTimeMillis() + max;
            ticks ++;
        }
        clientLevel.tick();
    }


    private boolean canCullTransparent(Chunk chunk){
        GridPoint3 pos = client.getPlayerChunkPos();
        int distance = Math.abs(chunk.localX - pos.x) + Math.abs(chunk.localY - pos.y) + Math.abs(chunk.localZ - pos.z);
        return distance > 14;
    }
    public ClientLevel getClientLevel() {
        return clientLevel;
    }


    public void addToMeshBuffers(ChunkMeshHolder meshHolder){
        if (meshHolder == null) return;
        var chunk = meshHolder.chunk;
        chunk.flagVisible = true;
        var terrain = meshHolder.terrainMesh;
        var liquid = meshHolder.waterMesh;
        var transparent = meshHolder.transparentMesh;
        handleMesh(terrain,chunk,terrainBuffer);
        handleMesh(liquid,chunk,liquidBuffer);
        handleMesh(transparent,chunk,transparentBuffer);
    }

    private void handleMesh(ChunkMesh newMesh,Chunk chunk, Array<ChunkMesh> meshes){
        if (newMesh == null){
            for (int i = meshes.size - 1; i >= 0; i--) {
                var chunkMesh = meshes.get(i);
                if (chunkMesh == null) continue;
                int oldX = chunkMesh.chunk.localX;
                int oldY = chunkMesh.chunk.localY;
                int oldZ = chunkMesh.chunk.localZ;
                if (oldX == chunk.localX && oldY == chunk.localY && oldZ == chunk.localZ){
                    chunkMesh.dispose();
                    meshes.removeIndex(i);
                }
            }
        } else {
            boolean addNew = true;
            if (meshes.size == 0){
                meshes.add(newMesh);
                return;
            }
            for (int i = meshes.size - 1; i >= 0; i--) {
                ChunkMesh chunkMesh = meshes.get(i);
                int oldX = chunkMesh.chunk.localX;
                int oldY = chunkMesh.chunk.localY;
                int oldZ = chunkMesh.chunk.localZ;
                if (oldX == chunk.localX && oldY == chunk.localY && oldZ == chunk.localZ){
                    chunkMesh.dispose();
                    meshes.set(i,newMesh);
                    addNew = false;
                }
            }
            if (addNew) {
                meshes.add(newMesh);
            }
        }
    }

    private boolean isOutRenderDistance(ChunkMesh chunkMesh,GridPoint3 pos){
        Chunk chunk = chunkMesh.chunk;
        int distanceX = Math.abs(chunk.localX - pos.x);
        int distanceY = Math.abs(chunk.localY - pos.y);
        int distanceZ = Math.abs(chunk.localZ - pos.z);
        return distanceX > RENDER_DISTANCE || distanceY > RENDER_DISTANCE || distanceZ > RENDER_DISTANCE;
    }


    public int getRenderBufferSize(){
        return terrainBuffer.size + transparentBuffer.size + liquidBuffer.size;
    }


    @Override
    public void dispose() {
        entities.forEach(Entity::dispose);
        terrainBuffer.forEach(ChunkMesh::dispose);
        liquidBuffer.forEach(ChunkMesh::dispose);
        transparentBuffer.forEach(ChunkMesh::dispose);
        skyRenderer.dispose();
    }

    public SkyRenderer getSkyRenderer() {
        return skyRenderer;
    }

    public Array<Entity> getEntities() {
        return entities;
    }
}
