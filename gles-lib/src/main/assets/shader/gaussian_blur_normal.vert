//From GPUImageGaussianBlurFilter
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 vTextureCoord;
uniform mat4 uMVPMatrix;

const int GAUSSIAN_SAMPLES = 9;
uniform float uWidthOffset;
uniform float uHeightOffset;
varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = aTextureCoord.xy;

    int multiplier = 0;
    vec2 blurStep;
    vec2 singleStepOffset = vec2(uWidthOffset, uHeightOffset);

    for (int i = 0; i < GAUSSIAN_SAMPLES; i++) {
        multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));
        blurStep = float(multiplier) * singleStepOffset;
        blurCoordinates[i] = aTextureCoord.xy + blurStep;
    }
}