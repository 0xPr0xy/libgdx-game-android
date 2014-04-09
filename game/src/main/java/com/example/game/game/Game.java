package com.example.game.game;

import android.util.Log;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by peterijlst on 28-03-14.
 */

public class Game implements ApplicationListener, InputProcessor {

    public PerspectiveCamera perspectiveCamera;
    public OrthographicCamera orthographicCamera;
    public Environment environment;
    public ModelBatch modelBatch;
    public ShapeRenderer shapeRenderer;
    public AssetManager assets;
    public FPSLogger fpsLog;
    public ModelInstance model;
    public CameraInputController camController;
    public Vector3 touchVector;
    public Vector3 modelVector;
    public Array<Vector3> transformArray = new Array<Vector3>();

    public boolean loading;
    public boolean dragging = false;
    public boolean shouldAnimate = false;
    public int animatedIndex;
    public float sceneWidth;
    public float sceneHeight;
    public float gridHeight;
    public float gridWidth;

    // static variables
    public final static boolean fpsLogEnabled = false;
    public final static boolean camControllerEnabled = false;
    public final static boolean perspectiveCameraEnabled = true;
    public final static String BALL_MODEL = "data/ball.g3db";
    public final static String LOGTAG = "Game";

    /*
    Game Lifecycle
    */

    @Override
    public void create () {
        modelBatch = new ModelBatch();

        shapeRenderer = new ShapeRenderer();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float sceneAspect = height / width;

        sceneWidth = width;
        sceneHeight = sceneWidth * sceneAspect;
        gridHeight = sceneHeight / (10 * sceneAspect);
        gridWidth = sceneWidth / 10;
        modelVector = new Vector3();
        touchVector = new Vector3();

        if(perspectiveCameraEnabled){
            perspectiveCamera = new PerspectiveCamera(67,sceneWidth,sceneHeight);
            perspectiveCamera.position.set(0,0,10);
            perspectiveCamera.lookAt(0,0,0);
            perspectiveCamera.near = 0.1f;
            perspectiveCamera.far = 300f;
            perspectiveCamera.update();
        } else {
            orthographicCamera = new OrthographicCamera(sceneWidth,sceneHeight);
            orthographicCamera.position.set(sceneWidth/2,sceneHeight/2,10);
            orthographicCamera.lookAt(sceneWidth/2,sceneHeight/2,0);
            orthographicCamera.setToOrtho(false);
            orthographicCamera.update();
        }

        if(camControllerEnabled){
            if(perspectiveCameraEnabled){
                camController = new CameraInputController(perspectiveCamera);
            } else {
                camController = new CameraInputController(orthographicCamera);
            }
            Gdx.input.setInputProcessor(camController);
        } else {
            Gdx.input.setInputProcessor(this);
        }

        if(fpsLogEnabled){
            fpsLog = new FPSLogger();
        }

        assets = new AssetManager();
        assets.load(BALL_MODEL, Model.class);
        loading = true;
    }

    private void doneLoading() {
        Model ball = assets.get(BALL_MODEL, Model.class);
        model = new ModelInstance(ball);
        loading = false;
    }

    @Override
    public void render () {

        if (loading && assets.update()){
            doneLoading();
        }

        // update camera controller if enabled
        getCamUpdate();

        // set viewport
        Gdx.gl.glViewport(0, 0, (int)sceneWidth, (int)sceneHeight);

        // set rgba values used when clearing color buffer
        Gdx.gl.glClearColor(0, 0.2f, 0, 1);

        // clear color buffer and depth buffer
        Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        // log FPS to console if enabled
        if(fpsLogEnabled){
            fpsLog.log();
        }

        // draw grid lines on X and Y axis
        drawGrid();

        // stop here if we don't have a modelInstance
        if(model == null){
            return;
        }

        // animate the model if there are new animations
        if(shouldAnimate){
            animateModel();
        }

        // begin render modelBatch
        if(perspectiveCameraEnabled){
            modelBatch.begin(perspectiveCamera);
        } else {
            modelBatch.begin(orthographicCamera);
        }
        // render model and environment
        modelBatch.render(model, environment);
        // stop render modelBatch
        modelBatch.end();

    }

    public void resize (int width, int height) {
    }

    public void pause () {
    }

    public void resume () {
    }

    @Override
    public void dispose () {
        shapeRenderer.dispose();
        modelBatch.dispose();
        assets.dispose();
    }

    /*
    Private methods
    */

    private void animateModel() {

        if(animatedIndex < transformArray.size){

            //get next model translation
            Vector3 animation = transformArray.get(animatedIndex);

            //get current model translation
            Vector3 tmp1 = new Vector3();
            model.transform.getTranslation(tmp1);

            // assign new height value for Z axis
            if(animatedIndex < transformArray.size/2){
                animation.z = tmp1.z += 0.1;
            } else if(animatedIndex > transformArray.size/2){
                animation.z = tmp1.z -= 0.1;
            }

            // calculate rotation direction
            int rotation = -360;
            model.transform.rotate(
                    Vector3.Z,
                    rotation * Gdx.graphics.getDeltaTime()
            );
            // translation
            model.transform.setTranslation(
                    animation
            );
            // increment animation index
            animatedIndex ++;
        } else {
            // animation sequence complete
            // reset values
            animatedIndex = 0;
            shouldAnimate = false;
            transformArray.clear();
        }
    }

    private void getCamUpdate(){
        if(camControllerEnabled){
            camController.update();
        } else {
            // update configured camera
            if(perspectiveCameraEnabled){
                perspectiveCamera.update();
            } else {
                orthographicCamera.update();
            }
        }
    }

    private void drawGrid() {
        // set line width
        Gdx.gl.glLineWidth(2);

        // begin render shapes with type line
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // render X axis grid
        shapeRenderer.setColor(Color.RED);
        for(int i = 0; i <= sceneHeight; i+= gridHeight){
            shapeRenderer.line(
                    0,
                    i,
                    sceneWidth,
                    i
            );
        }
        // render Y axis grid
        shapeRenderer.setColor(Color.GREEN);
        for(int i = 0; i <= sceneWidth; i+= gridWidth){
            shapeRenderer.line(
                    i,
                    0,
                    i,
                    sceneHeight
            );
        }
        // stop render shapes
        shapeRenderer.end();
    }

    /*
    Input Processor delegate methods
    */

    @Override
    public boolean keyDown(int i) {
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int i, int i2, int i3, int i4) {
        Log.d(LOGTAG,"touchDown");
        if(transformArray.size > 0){
            transformArray.clear();
            animatedIndex = 0;
        }
        return false;
    }

    @Override
    public boolean touchUp(int i, int i2, int i3, int i4) {
        Log.d(LOGTAG,"touchUp");
        if(transformArray.size > 0){
            shouldAnimate = true;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int i, int i2, int i3) {
        Log.d(LOGTAG,"touchDragged");
        if(!dragging){
            dragging = true;
            Vector3 touch = new Vector3();
            Ray ray = (perspectiveCameraEnabled) ? perspectiveCamera.getPickRay(i, i2) : orthographicCamera.getPickRay(i, i2);
            model.transform.getTranslation(modelVector);
            final float distance = -ray.origin.z / ray.direction.z;
            touch.set(ray.direction).scl(distance).add(ray.origin);
            touch.z = 0;
            transformArray.add(touchVector);
            touchVector = touch;
            dragging = false;
        }

        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i2) {
        return false;
    }

    @Override
    public boolean scrolled(int i) {
        return false;
    }
}