precision mediump float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;
const int SHIFT_SIZE = 2;
varying vec4 blurShiftCoordinates[SHIFT_SIZE];

void main() {
    // 计算当前坐标的颜色值
    vec4 currentColor = texture2D(uTextureUnit, vTextureCoord);
    mediump vec3 sum = currentColor.rgb;
    // 计算偏移坐标的颜色值总和
    for (int i = 0; i < SHIFT_SIZE; i++) {
        sum += texture2D(uTextureUnit, blurShiftCoordinates[i].xy).rgb;
        sum += texture2D(uTextureUnit, blurShiftCoordinates[i].zw).rgb;
    }
    // 求出平均值
    gl_FragColor = vec4(sum * 1.0 / float(2 * SHIFT_SIZE + 1), currentColor.a);
}