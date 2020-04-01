precision highp float;
uniform sampler2D uTextureUnit;
varying vec2 vTextureCoord;

uniform int repeat; // 画面重复的次数

void main() {
    vec2 uv = vTextureCoord;
    // 反向UV坐标
    vec2 invertedUV = 1.0 - uv;
    // 计算重复次数之后的uv值以及偏移值
    vec2 fiter = floor(uv * float(repeat) * 2.0);
    vec2 riter = floor(invertedUV * float(repeat) * 2.0);
    vec2 iter = min(fiter, riter);
    float minOffset = min(iter.x, iter.y);
    // 偏移值
    vec2 offset = (vec2(0.5, 0.5) / float(repeat)) * minOffset;
    // 当前实际的偏移值
    vec2 currenOffset = 1.0 / (vec2(1.0, 1.0) - offset * 2.0);
    // 计算出当前的实际UV坐标
    vec2 currentUV = (uv - offset) * currenOffset;

    gl_FragColor = texture2D(uTextureUnit, fract(currentUV));
}