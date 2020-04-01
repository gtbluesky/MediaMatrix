// 边框模糊
precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;
uniform sampler2D uBlurTextureUnit;
uniform float blurOffset;// 边框模糊偏移值：[0.0, 0.5)

void main() {
    vec2 uv = vTextureCoord.xy;
    vec4 color;
    if (uv.x >= blurOffset
        && uv.x <= 1.0 - blurOffset
        && uv.y >= blurOffset
        && uv.y <= 1.0 - blurOffset
    ) {
        // 整体与内容区域的比值
        float scale = 1.0 / (1.0 - 2.0 * blurOffset);
        uv = vec2((uv.x - blurOffset) * scale, (uv.y - blurOffset) * scale);
        color = texture2D(uTextureUnit, uv);
    } else {
        color = texture2D(uBlurTextureUnit, uv);
    }
    gl_FragColor = color;
}