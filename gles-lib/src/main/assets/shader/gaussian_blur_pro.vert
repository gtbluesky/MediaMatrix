attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 vTextureCoord;
uniform mat4 uMVPMatrix;

const int SHIFT_SIZE = 2;
uniform float uWidthOffset;
uniform float uHeightOffset;
varying vec4 blurShiftCoordinates[SHIFT_SIZE];

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = aTextureCoord.xy;

    // 偏移步距
    vec2 singleStepOffset = vec2(uWidthOffset, uHeightOffset);
    // 记录偏移坐标
    for (int i = 0; i < SHIFT_SIZE; i++) {
        blurShiftCoordinates[i] = vec4(
            aTextureCoord.xy - float(i + 1) * singleStepOffset,
            aTextureCoord.xy + float(i + 1) * singleStepOffset
        );
    }
}