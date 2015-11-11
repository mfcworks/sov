
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SOV {
	// パラメータ群
	public final int N = 200; // セル数
	public final int init = 30; // 初期の車の台数
	public final double a = 0.01;
	public final double c = 1.5;
	public double alpha; // 生成確率
	public double beta; // 消滅確率
	public List<Integer> x; // 車の位置のリスト
	public List<Double> v; // 車の速度のリスト
	private Random rand;

	// コンストラクタ
	public SOV(double alpha, double beta) {
		this.alpha = alpha;
		this.beta = beta;

		rand = new Random();

		// (リンクリストのほうが賢いのかと一瞬思ったりしたが)
		// ランダムアクセスにコストがかからないArrayListのほうが良い
		x = new ArrayList<>();
		v = new ArrayList<>();

		initialize();
	}

	// 初期状態の生成
	public void initialize() {

	}

	// 時間発展
	// @return 動いた車の数/全台数
	public double update() {
		int moved = 0; // 動いた車の数

		// 先頭車両以外の車について：
		for (int i = 0; i < x.size() - 1; i++) {
			// 前の車との距離
			int dx = x.get(i + 1) - x.get(i);
			// この車の次の時間での速度
			double vel = (1 - a) * v.get(i) + a * V(dx - 1);
			v.set(i, vel);

			if (dx > 1 && rand.nextDouble() < vel) {
				x.set(i, x.get(i) + 1);
				moved++;
			}
		}
		// 先頭車両について：
		int first = x.size() - 1;
		if (first >= 0) { // 車が1台もいなかったら実行しない
			v.set(first, 1.0);
			if (x.get(first) != N - 1) {
				x.set(first, x.get(first) + 1);
				moved++;
			}
		}
		return (x.size() == 0) ? 0 : (((double) moved) / x.size());
	}

	// 境界条件の処理
	public void boundary() {
		// 車の生成
		if (((x.size() > 0 && x.get(0) != 0) || x.size() == 0) && rand.nextDouble() < alpha) {
			x.add(0, 0);
			v.add(0, 1.0);
		}
		// 車の消滅
		int first = x.size() - 1;
		if (first >= 0 && x.get(first) == N - 1 && rand.nextDouble() < beta) {
			x.remove(first);
			v.remove(first);
		}
	}

	// 最適速度関数
	public double V(double dx) {
		return (Math.tanh(dx - c) + Math.tanh(c))
				/ (1 + Math.tanh(c));
	}

	public String plot() {
		StringBuilder sb = new StringBuilder(x.size());
		int i = 0;
		for (int j = 0; j < N; j++) {
			if (i < x.size() && x.get(i) == j) {
				sb.append("*");
				i++;
			} else
				sb.append("-");
		}
		return sb.toString();
	}

	// 平均速度：全ての車の速度の平均値
	public double velocity() {
		double vel = 0;
		for (int i = 0; i < v.size(); i++) {
			vel += v.get(i);
		}
		return (v.size() == 0) ? 0 : (vel / v.size());
	}

	// 密度
	public double density() {
		return ((double) x.size()) / N;
	}

	// シミュレーション
	public static double[] simulate(double alpha, double beta, int ignore, int updateMax) {
		SOV model = new SOV(alpha, beta);
		// 最初のignore回は無視される
		for (int i = 0; i < ignore; i++) {
			model.update();
			model.boundary();
		}
		// 続くupdateMax回のデータの平均値を返す：
		double vel = 0, cntVel = 0, dens = 0, flux = 0, cntFlux = 0;

		for (int i = 0; i < updateMax; i++) {
			model.update();
			model.boundary();

			// データ取得
			double vel_ = model.velocity();
			double dens_ = model.density();
			double cntVel_ = model.update();

			vel += vel_;
			dens += dens_;
			cntVel += cntVel_;
			flux += vel_ * dens_;	// フラックスと計数フラックスを各vel*densの平均とする
			cntFlux += cntVel_ * dens_;
		}
		vel /= updateMax;
		dens /= updateMax;
		cntVel /= updateMax;
		flux /= updateMax;
		cntFlux /= updateMax;

		// 返り値：{密度、速度、フラックス、計数速度、計数フラックス}
		return new double[]{dens, vel, flux, cntVel, cntFlux};
	}

	/// test ///
	public static void main(String[] args) {
		try {
			FileWriter fw = new FileWriter("data.csv");
			String br = System.getProperty("line.separator");
			fw.write("Alpha, Beta, Density, Flux" + br);

			int ignore = 100;
			int updateMax = 2000;
			int samples = 10;

			for (double alpha = 0; alpha < 1; alpha += 0.05) {
				for (double beta = 0; beta < 1; beta += 0.05) {

					for (int i = 0; i < samples; i++) {
						double[] res = simulate(alpha, beta, ignore, updateMax);
						double dens = res[0];
						double flux = res[4];
						System.out.println(alpha + ", " + beta + ", " + dens + ", " + flux);
						fw.write(alpha + ", " + beta + ", " + dens + ", " + flux + br);
					}
				}
			}

			fw.close();
			System.out.println("正常終了");

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	/// test ///
	public static void main2(String[] args) {
		try {
			FileWriter fw = new FileWriter("data.csv");
			String br = System.getProperty("line.separator");
			fw.write("Step, Density, Velocity" + br);

			SOV model = new SOV(0.2, 0.6);
			for (int i = 0; i < 20000; i++) {
				double vel = model.velocity();
				double dens = model.density();
				model.update();
				model.boundary();
				fw.write(i + ", " + dens + ", " + vel + br);
			}

			fw.close();
			System.out.println("正常終了");

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}


}
