// 仿抖音抖动特效
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;

uniform float scale;

void main() {
    vec2 uv = vTextureCoord.xy;
    vec2 center = vec2(0.5, 0.5);
    vec2 scaleCoordinate = (uv - center) / scale + center;
    vec4 smoothColor = texture2D(uTextureUnit, scaleCoordinate);

    // 计算红色通道偏移值
    vec4 shiftRedColor = texture2D(
        uTextureUnit,
        scaleCoordinate + vec2(
            -0.1 * (scale - 1.0),
            - 0.1 * (scale - 1.0)
        )
    );

    // 计算绿色通道偏移值
    vec4 shiftGreenColor = texture2D(
        uTextureUnit,
        scaleCoordinate + vec2(
            -0.075 * (scale - 1.0),
            - 0.075 * (scale - 1.0)
        )
    );

    // 计算蓝色偏移值
    vec4 shiftBlueColor = texture2D(
        uTextureUnit,
        scaleCoordinate + vec2(
            -0.05 * (scale - 1.0),
            - 0.05 * (scale - 1.0)
        )
    );

    vec3 resultColor = vec3(shiftRedColor.r, shiftGreenColor.g, shiftBlueColor.b);

    gl_FragColor = vec4(resultColor, smoothColor.a);
}